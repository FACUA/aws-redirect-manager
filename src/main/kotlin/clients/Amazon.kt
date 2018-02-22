package clients

import com.amazonaws.services.route53.AmazonRoute53ClientBuilder
import com.amazonaws.services.route53.model.*
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.*
import com.amazonaws.waiters.WaiterParameters
import extensions.endpoint
import extensions.hostedZone
import extensions.rootDomain
import io.reactivex.Observable
import support.task

object Amazon {
	private val route53 = AmazonRoute53ClientBuilder.defaultClient()
	private val s3 = AmazonS3ClientBuilder.defaultClient()

	private var cachedHostedZones: List<HostedZone>? = null

	fun getHostedZones(): Observable<HostedZone> = task {
		cachedHostedZones ?: {
			cachedHostedZones = route53.listHostedZones().hostedZones
			cachedHostedZones
		}()
	}
		.flatMapIterable { it }

	fun getHostedZoneRecordSets(
		hostedZone: HostedZone
	): Observable<ResourceRecordSet> = task {
		route53.listResourceRecordSets(
			ListResourceRecordSetsRequest()
				.withHostedZoneId(hostedZone.id)
		).resourceRecordSets
	}
		.flatMapIterable { it }

	fun createHostedZoneRecordSet(domain: String) = task {
		val zone = getZoneByDomain(domain)

		val recordSet = ResourceRecordSet()
			.withName("$domain.")
			.withType(RRType.A)
			.withAliasTarget(
				AliasTarget()
					.withDNSName("${s3.endpoint}.")
					.withHostedZoneId(s3.hostedZone)
					.withEvaluateTargetHealth(false)
			)

		route53.changeResourceRecordSets(
			ChangeResourceRecordSetsRequest()
				.withHostedZoneId(zone.id)
				.withChangeBatch(
					ChangeBatch()
						.withChanges(
							Change(
								ChangeAction.CREATE,
								recordSet
							)
						)
				)
		)
	}

	fun deleteHostedZoneRecordSet(domain: String) = task {
		val zone = getZoneByDomain(domain)

		val recordSet = ResourceRecordSet()
			.withName("$domain.")
			.withType(RRType.A)
			.withAliasTarget(
				AliasTarget()
					.withDNSName("${s3.endpoint}.")
					.withHostedZoneId(s3.hostedZone)
					.withEvaluateTargetHealth(false)
			)

		route53.changeResourceRecordSets(
			ChangeResourceRecordSetsRequest()
				.withHostedZoneId(zone.id)
				.withChangeBatch(
					ChangeBatch()
						.withChanges(
							Change(
								ChangeAction.DELETE,
								recordSet
							)
						)
				)
		)
	}

	fun getBucketWebsite(name: String) = task {
		s3.getBucketWebsiteConfiguration(name)
	}

	fun createBucketWithRedirect(
		name: String,
		redirectProtocol: String,
		redirectTarget: String
	) = task {
		try {
			s3.createBucket(
				CreateBucketRequest(name)
					.withCannedAcl(CannedAccessControlList.PublicRead)
			)
		} catch (e: AmazonS3Exception) {
			/*
			 * If the exception is:
			 * "A conflicting conditional operation is currently in
			 * progress against this resource. Please try again."
			 * we can ignore it.
			 */
			if (e.statusCode != 409) {
				throw e
			}
		}

		// Wait for the bucket to be created
		s3
			.waiters()
			.bucketExists()
			.run(WaiterParameters(HeadBucketRequest(name)))

		s3.setBucketWebsiteConfiguration(
			SetBucketWebsiteConfigurationRequest(
				name,
				BucketWebsiteConfiguration()
					.withRedirectAllRequestsTo(
						RedirectRule()
							.withProtocol(redirectProtocol)
							.withHostName(redirectTarget)
					)
			)
		)
	}

	fun deleteBucket(name: String) = task {
		s3.deleteBucket(name)

		s3
			.waiters()
			.bucketNotExists()
			.run(WaiterParameters(HeadBucketRequest(name)))
	}

	private fun getZoneByDomain(domain: String) = cachedHostedZones
		?.find { it.name == "${domain.rootDomain}." }
		?: throw Error("There isn't any hosted zone for ${domain.rootDomain}")

}