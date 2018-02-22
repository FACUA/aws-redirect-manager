package commands

import clients.Manager
import extensions.toJSONArray
import model.Redirection
import model.toJSON
import support.errorHandler

fun list(jsonOutput: Boolean) {
	errorHandler(
		Manager
			.getAllRedirections()
			.toList()
			.map {
				if (jsonOutput) {
					it.map(Redirection::toJSON).toJSONArray().toString(4)
				} else {
					it.joinToString("\n") { "${it.fromFQDN} --> ${it.toURI}" }
				}
			}
	)
}

fun listHelp() {
	println(
		"""Lists all existing redirections.
			|
			|This program considers a redirection when there is an existing S3
			|bucket with the FQDN of the redirection as its name, it has a
			|website enabled that redirects to the target URI, and an ALIAS A
			|record points to that bucket from a hosted zone.
			|
			|Usage: aws-redirect-manager list [--json]
			|
			|Options:
			|
			|   --json    Outputs the result in JSON format.
		""".trimMargin()
	)
}