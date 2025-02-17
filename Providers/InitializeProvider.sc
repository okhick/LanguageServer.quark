// https://microsoft.github.io/language-server-protocol/specifications/specification-current/#initialize
InitializeProvider : LSPProvider {
	var initializeParams;

	*methodNames { ^["initialize"] }
	*clientCapabilityName { ^nil }
	*serverCapabilityName { ^nil }

	init {
	}

	options {
		// https://microsoft.github.io/language-server-protocol/specifications/specification-3-17/#clientCapabilities
		^(
			// @TODO Fetch these from LSPCompletionHandler
			triggerCharacters: [".", "(", "~"],

			// @TODO These are overridden by commit chars for each completion - do we need?
			allCommitCharacters: [],

			resolveProvider: false,
			completionItem: (
				labelDetailsSupport: true
			)
		)
	}

	onReceived {
		|method, params|
		var serverCapabilities;

		initializeParams = params;

		serverCapabilities = ();
		this.addProviders(initializeParams["capabilities"], serverCapabilities);
		Log('LanguageServer.quark').info("Server capabilities are: %", serverCapabilities);

		^(
			"serverInfo": server.serverInfo,
			"capabilities": serverCapabilities;
		);
	}

	addProviders {
		|clientCapabilities, serverCapabilities, pathRoot=([])|
		var allProviders = LSPFeature.all;

		Log('LanguageServer.quark').info("Found providers: %", allProviders.collect(_.methodNames).join(", "));

		allProviders.do {
			|providerClass|
			var provider, clientCapability;

			// If clientCapabilityName.isNil, assume we ALWAYS use this provider
			clientCapability = providerClass.clientCapabilityName !? {
				this.getClientCapability(clientCapabilities, providerClass.clientCapabilityName)
			} ?? { () };

			clientCapability !? {
				|capability|
				Log('LanguageServer.quark').info("Registering provider: %", providerClass.methodNames);

				provider = providerClass.new(server, capability);

				providerClass.serverCapabilityName !? {
					|capabilityName|
					this.addServerCapability(
						serverCapabilities,
						capabilityName,
						provider.options
					)
				};	

				server.addProvider(provider);
			}
		}
	}

	getClientCapability {
		|clientCapabilities, path|
		Log('LanguageServer.quark').info("Checking for client capability at % (clientCapabilities: %)", path, clientCapabilities);

		if (path.isNil) { ^() };

		path.split($.).do {
			|key|
			if (clientCapabilities.isNil or: { clientCapabilities.isKindOf(Dictionary).not }) {
				^nil
			} {
				clientCapabilities = clientCapabilities[key]
			}
		};

		^clientCapabilities
	}

	addServerCapability {
		|serverCapabilities, path, options|
		Log('LanguageServer.quark').info("Adding server capability at %: %", path, options);

		if (path.isNil) { ^this };

		if (options.notNil) {
			path = path.split($.).collect(_.asSymbol);

			if (path.size > 1) {
				path[0..(path.size-2)].do {
					|key|
					Log('LanguageServer.quark').info("looking up key %", key);
					serverCapabilities[key] = serverCapabilities[key] ?? { () };
					serverCapabilities = serverCapabilities[key];
				};
			};

			Log('LanguageServer.quark').info("writing options into key %", path.last);
			serverCapabilities[path.last] = options;
		}
	}
}
