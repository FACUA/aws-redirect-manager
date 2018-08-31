package clients

import com.amazonaws.regions.Regions
import com.amazonaws.services.certificatemanager.AWSCertificateManagerClientBuilder
import com.amazonaws.services.certificatemanager.model.CertificateStatus
import com.amazonaws.services.certificatemanager.model.CertificateSummary
import com.amazonaws.services.certificatemanager.model.ListCertificatesRequest
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder
import com.amazonaws.services.cloudfront.model.*
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder
import com.amazonaws.services.route53.model.*
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.*
import com.amazonaws.waiters.WaiterParameters
import extensions.endpoint
import extensions.hostedZone
import extensions.rootDomain
import extensions.toOptional
import io.reactivex.Observable
import support.task
import java.util.*

object Amazon {
	private val route53 = AmazonRoute53ClientBuilder.defaultClient()
	private val s3 = AmazonS3ClientBuilder.defaultClient()
	private val acm = AWSCertificateManagerClientBuilder
		.standard()
		.withRegion(Regions.US_EAST_1)
		.build()
	private val cloudfront = AmazonCloudFrontClientBuilder
		.standard()
		.withRegion(Regions.EU_WEST_1)
		.build()

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

	fun getCertificateForDomain(domain: String) = task {
		val certs = acm.listCertificates(
			ListCertificatesRequest()
				.withCertificateStatuses(
					CertificateStatus.ISSUED
				)
		)
			.certificateSummaryList

		certs
			.find {
				val rootDomain = domain.rootDomain

				// If we have a root domain, find a certificate with that
				// domain. If we have a subdomain, find a certificate with
				// that domain, or a wildcard certificate.
				if (rootDomain == domain) {
					it.domainName == domain
				} else {
					it.domainName == domain ||
						it.domainName == "*.$rootDomain"
				}
			}
			.toOptional()
	}

	fun createCloudfrontDistribution(
		s3BucketName: String,
		acmCertificate: CertificateSummary
	) = task {
//		val domain = "$s3BucketName.s3-website-eu-west-1.amazonaws.com"
		val domain = "$s3BucketName.s3.amazonaws.com"
		val originId = "S3-$domain"
//		val originId = "S3-Website-$domain"
//		val originId = "Custom-$domain"
		val origin = Origin()
			.withId(originId)
//			.withCustomOriginConfig(
//				CustomOriginConfig()
//					.withHTTPPort(80)
//					.withHTTPSPort(443)
//					.withOriginProtocolPolicy(OriginProtocolPolicy.HttpOnly)
//			)
			.withS3OriginConfig(
				S3OriginConfig()
					.withOriginAccessIdentity("origin-access-identity/cloudfront/E2X22O95ERLHWD")
			)
			.withDomainName(domain)

		val behavior = DefaultCacheBehavior()
			.withTargetOriginId(originId)
			.withTrustedSigners(
				TrustedSigners()
					.withQuantity(0)
					.withEnabled(false)
			)
			.withMinTTL(0)
			.withMaxTTL(31536000)
			.withDefaultTTL(86400)
			.withViewerProtocolPolicy(ViewerProtocolPolicy.RedirectToHttps)
			.withForwardedValues(
				ForwardedValues()
					.withCookies(
						CookiePreference()
							.withForward(ItemSelection.None)
					)
					.withQueryString(false)
			)

		cloudfront
			.createDistribution(
				CreateDistributionRequest()
					.withDistributionConfig(
						DistributionConfig()
							.withOrigins(
								Origins()
									.withQuantity(1)
									.withItems(origin)
							)
							.withAliases(
								Aliases()
									.withQuantity(1)
									.withItems(s3BucketName)
							)
							.withViewerCertificate(
								ViewerCertificate()
									.withACMCertificateArn(
										acmCertificate.certificateArn
									)
							)
							.withDefaultCacheBehavior(behavior)
							.withComment(
								"Created by aws-redirect-manager"
							)
							.withEnabled(true)
							.withCallerReference(
								Calendar
									.getInstance()
									.timeInMillis
									.toString()
							)
					)
					.let { println(it); it }
			)
			.distribution
	}

	private fun getZoneByDomain(domain: String) = cachedHostedZones
		?.find { it.name == "${domain.rootDomain}." }
		?: throw Error("There isn't any hosted zone for ${domain.rootDomain}")

}