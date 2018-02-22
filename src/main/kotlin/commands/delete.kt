package commands

import clients.Manager
import support.errorHandler

fun delete(domain: String) {
	errorHandler(
		Manager.deleteRedirection(domain).map { "" }
	)
}

fun deleteHelp() {
	println(
		"""Deletes an existing redirection
			|
			|Deletes the S3 bucket and hosted zone record associated with
			|the redirection.
			|
			|Usage: aws-redirect-manager delete (FQDN)
			|
			|Parameters:
			|
			|   FQDN      The fully qualified domain name of the redirection
		""".trimMargin()
	)
}