package commands

import clients.Amazon
import clients.Manager
import com.amazonaws.services.certificatemanager.model.CertificateSummary
import extensions.rootDomain
import extensions.toNullable
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import model.Redirection
import support.errorHandler

fun enableHttps(domain: String) {
	val observable = Observable.combineLatest<
		CertificateSummary,
		Redirection,
		Pair<CertificateSummary, Redirection>
	>(
		Amazon
			.getCertificateForDomain(domain)
			.map {
				it.toNullable()
					?: throw Exception("No certificate present for $domain")
			},
		Manager.getRedirections(domain.rootDomain, domain)
			.toList()
			.map {
				val redirection = it.singleOrNull()
					?: throw Exception("No redirection found for $domain")

				// TODO
				if (redirection.isHttps) {
					throw Exception("The redirection for domain $domain has " +
						"already HTTPS enabled")
				}

				redirection
			}
			.toObservable(),
		BiFunction { cert, redirection -> cert to redirection }
	)
		.flatMap { (cert, redirection) ->
			Amazon.createCloudfrontDistribution(
				redirection.fromFQDN,
				cert
			)
		}

	errorHandler(observable.map { "" })
}

fun enableHttpsHelp() {
	// TODO document rate limit

	println(
		"""Enables HTTPS for an existing HTTP redirection
			|
			|Creates a CloudFront Distribution that serves the redirection from
			|the S3 bucket, disables public access on the bucket, and replaces
			|the Route 53 record to point to CloudFront instead of S3.
			|
			|Requires an Amazon Certificate Manager certificate to exist in the
			|us-east-1 (N. Virginia) region for the domain name or subdomain
			|name. If the FQDN is a subdomain, wildcard certificates can also be
			|used. For example:
			|
			|   * example.com requires a certificate for example.com
			|   * foo.example.com requires either:
			|       * a certificate for foo.example.com
			|       * a certificate for *.example.com
			|
			|The new CloudFront endpoint will listen to both HTTP and HTTPS,
			|and will redirect HTTP to HTTPS.
			|
			|This is a good practice and should always be enabled whenever
			|possible.
			|
			|Usage: aws-redirect-manager enable-https (FQDN)
			|
			|Parameters:
			|
			|   FQDN      The fully qualified domain name of the redirection
		""".trimMargin()
	)
}