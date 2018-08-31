package commands

import clients.Manager
import extensions.toJSONArray
import model.Redirection
import model.toJSON
import support.errorHandler

fun list(jsonOutput: Boolean, rootDomain: String? = null) {
	errorHandler(
		Manager
			.getRedirections(rootDomain)
			.toList()
			.map { redirections ->
				if (jsonOutput) {
					redirections
						.map(Redirection::toJSON)
						.toJSONArray()
						.toString(4)
				} else {
					redirections
						.joinToString("\n") {
							"${it.fromFQDN} --> ${it.toURI}"
						}
				}
			}
	)
}

fun listHelp() {
	println(
		"""Lists existing redirections.
			|
			|This program considers a redirection when there is an existing S3
			|bucket with the FQDN of the redirection as its name, it has a
			|website enabled that redirects to the target URI, and an ALIAS A
			|record points to that bucket from a hosted zone.
			|
			|Usage: aws-redirect-manager list [domain] [--json]
			|
			|Parameters
			|
			|   domain    If specified, only redirections from this domain
			|             will be listed. Otherwise, all existing redirections
			|             will be listed (may take a long time).
			|
			|Options:
			|
			|   --json    Outputs the result in JSON format.
		""".trimMargin()
	)
}