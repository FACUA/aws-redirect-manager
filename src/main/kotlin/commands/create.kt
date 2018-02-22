package commands

import clients.Manager
import support.errorHandler

fun create(fromFQDN: String, toURI: String) {
	val (protocol, uri) = """(http|https)://(.+)"""
		.toRegex()
		.matchEntire(toURI)
		?.destructured
		?: throw Error("The uri $toURI is not valid!\n" +
			"Make sure that is in the form of: http(s)://www.example.com")

	errorHandler(
		Manager.createRedirection(
			fromFQDN,
			protocol,
			uri
		)
			.map { "" }
	)
}

fun createHelp() {
	println(
		"""Creates a new redirection
			|
			|Creates an empty S3 bucket hosting a website that will redirect
			|to the specified URI, and a hosted zone record that points to
			|that bucket.
			|
			|Usage: aws-redirect-manager create (fromFQDN) (toURI)
			|
			|Parameters:
			|
			|   fromFQDN  The fully qualified domain name that will be the
			|             beginning of the web redirection. For example:
			|             admin.example.com
			|   toURI     The URI where the FQDN will point. For example:
			|             https://example.com/admin
		""".trimMargin()
	)
}