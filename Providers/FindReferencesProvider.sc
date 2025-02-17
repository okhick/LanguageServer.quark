// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_implementation
FindReferencesProvider : LSPProvider {
	*methodNames {
		^[
			"textDocument/references",
		]
	}
	*clientCapabilityName { ^"textDocument.references" }
	*serverCapabilityName { ^"referencesProvider" }

	init {
		|clientCapabilities|
	}

	options {
		^()
	}

	onReceived {
		|method, params|
		Log('LanguageServer.quark').info("Handling: %", method);

		^nil
	}
}
