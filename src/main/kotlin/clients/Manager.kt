package clients

import extensions.isS3Redirection
import extensions.s3BucketName
import extensions.uri
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import model.Redirection

object Manager {
	fun getRedirections(
		rootDomain: String? = null
	): Observable<Redirection> = Amazon.getHostedZones()
		// If the root domain is null, get all redirections
		// If the root domain is not null, get only the redirections
		// for that domain
		.filter { rootDomain == null || it.name == "$rootDomain." }
		.flatMap { Amazon.getHostedZoneRecordSets(it) }
		.filter { it.type == "A" }
		.filter { it.aliasTarget != null }
		.filter { it.aliasTarget.dnsName.isS3Redirection }
		.map { it.name.s3BucketName }
		.flatMap {
			val bucketName = it
			Amazon.getBucketWebsite(bucketName)
				.map { Pair(bucketName, it) }
		}
		.filter { (_, website) -> website.redirectAllRequestsTo != null }
		.map { (bucketName, website) ->
			Redirection(bucketName, website.redirectAllRequestsTo.uri)
		}

	fun createRedirection(
		fromDomain: String,
		protocol: String,
		to: String
	): Observable<Redirection> = Amazon.getHostedZones()
		.take(1)
		.flatMap { Amazon.createBucketWithRedirect(fromDomain, protocol, to) }
		.flatMap { Amazon.createHostedZoneRecordSet(fromDomain) }
		.map { Redirection(fromDomain, "$protocol://$to") }

	fun deleteRedirection(
		domain: String
	): Observable<Unit> = Amazon.getHostedZones()
		.take(1)
		.flatMap {
			Observable.combineLatest<Unit, Unit, Unit>(
				Amazon.deleteBucket(domain).map {},
				Amazon.deleteHostedZoneRecordSet(domain).map {},
				BiFunction { _, _ -> Unit }
			)
		}
}