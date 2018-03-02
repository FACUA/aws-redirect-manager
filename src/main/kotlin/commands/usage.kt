package commands

fun usage() {
	println(
		"""aws-redirect-manager v1.1
			|A tool to manage Route 53 web redirections.
			|
			|Usage: aws-redirect-manager [command]
			|
			|Available commands are:
			|
			|   list        Lists all existing redirections
			|   create      Creates a new redirection
			|   bulk-create Creates redirections in bulk
			|   delete      Deletes an existing redirection
			|
			|Run a command with --help to get further help.
		""".trimMargin()
	)
}