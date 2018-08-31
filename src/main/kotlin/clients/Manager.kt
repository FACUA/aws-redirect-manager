package clients

import extensions.isS3Redirection
import extensions.rootDomain
import extensions.s3BucketName
import extensions.uri
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import model.Redirection

object Manager {
	fun getRedirections(
		rootDomain: String? = null,
		domain: String? = null
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
		// If the redirection domain is null, get all redirections
		// If the redirection domain is not null, get only this redirection
		.filter { domain == null || it == domain }
		.flatMap { bucketName ->
			Amazon.getBucketWebsite(bucketName)
				.map { bucketName to it }
		}
		.filter { (_, website) -> website.redirectAllRequestsTo != null }
		.map { (bucketName, website) ->
			Redirection(
				bucketName,
				website.redirectAllRequestsTo.uri,
				false // TODO
			)
		}

	fun createRedirection(
		fromDomain: String,
		protocol: String,
		to: String
	): Observable<Redirection> = Amazon.getHostedZones()
		.take(1)
		.flatMap { Amazon.createBucketWithRedirect(fromDomain, protocol, to) }
		.flatMap { Amazon.createHostedZoneRecordSet(fromDomain) }
		.map {
			Redirection(
				fromDomain,
				"$protocol://$to",
				false // TODO
			)
		}

	fun deleteRedirection(
		domain: String
	): Observable<Unit> = Amazon.getHostedZones()
		.take(1)
		.flatMap {
			Observable.combineLatest<Any, Any, Unit>(
				Amazon.deleteBucket(domain),
				Amazon.deleteHostedZoneRecordSet(domain),
				BiFunction { _, _ -> Unit }
			)
		}
}