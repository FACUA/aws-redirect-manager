package commands

object CommandManager {
	fun run(args: Array<String>) = if (args.isEmpty()) {
		usage()
	} else {
		val help = args.find { it == "--help" } != null
		val jsonOutput = args.find { it == "--json" } != null

		when (args[0]) {
			"list" -> if (help) {
				listHelp()
			} else {
				list(
					jsonOutput,
					if (args.size >= 2 && args[1].substring(0, 2) != "--") {
						args[1]
					} else {
						null
					}
				)
			}
			"create" -> if (help) {
				createHelp()
			} else {
				if (args.size < 3) {
					println("Error: not enough arguments specified.\n")
					createHelp()
				} else {
					create(args[1].trim(), args[2].trim())
				}
			}
			"bulk-create" -> if (help) {
				bulkCreateHelp()
			} else {
				bulkCreate()
			}
			"delete" -> if (help) {
				deleteHelp()
			} else {
				if (args.size < 2) {
					println("Error: not enough arguments specified.\n")
					deleteHelp()
				} else {
					delete(args[1].trim())
				}
			}
			else -> usage()
		}
	}
}