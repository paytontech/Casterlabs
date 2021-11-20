{
	// Dependencies.

	function EventHandler() {
		let listeners = {};
		let callbackIdCounter = 0;

		return {
			on(type, callback) {
				const callbackId = callbackIdCounter++;

				type = type.toLowerCase();

				let callbacks = listeners[type] || {};

				callbacks[callbackId] = callback;

				listeners[type] = callbacks;

				return callbackId;
			},

			once(type, callback) {
				const callbackId = callbackIdCounter++;

				type = type.toLowerCase();

				let callbacks = listeners[type] || {};

				callbacks[callbackId] = function (data) {
					delete listeners[type][callbackId];
					callback(data);
				};

				listeners[type] = callbacks;

				return callbackId;
			},

			off(type, callbackId) {
				delete listeners[type][callbackId];
			},

			broadcast(type, data, clone = true) {
				// Broadcast under a wildcard.
				{
					const wildCardCallbacks = listeners["*"];

					if (wildCardCallbacks) {
						Object.values(wildCardCallbacks).forEach((callback) => {
							try {
								if (clone) {
									callback(type.toLowerCase(), Object.assign({}, data));
								} else {
									callback(type.toLowerCase(), data);
								}
							} catch (e) {
								console.error("A listener produced an exception: ");
								console.error(e);
							}
						});
					}
				}

				// Broadcast under type.
				{
					const callbacks = listeners[type.toLowerCase()];

					if (callbacks) {
						Object.values(callbacks).forEach((callback) => {
							try {
								if (clone) {
									callback(Object.assign({}, data));
								} else {
									callback(data);
								}
							} catch (e) {
								console.error("A listener produced an exception: ");
								console.error(e);
							}
						});
					}
				}
			}
		};
	}

	function ThrowawayEventHandler() {
		let listeners = {};
		let callbackIdCounter = 0;

		let throwawyCallbackId;

		const instance = {
			on(type, callback) {
				const callbackId = callbackIdCounter++;

				type = type.toLowerCase();

				let callbacks = listeners[type] || {};

				callbacks[callbackId] = callback;

				listeners[type] = callbacks;

				return callbackId;
			},

			once(type, callback) {
				const callbackId = callbackIdCounter++;

				type = type.toLowerCase();

				let callbacks = listeners[type] || {};

				callbacks[callbackId] = function (data) {
					delete listeners[type][callbackId];
					callback(data);
				};

				listeners[type] = callbacks;

				return callbackId;
			},

			off(type, callbackId) {
				delete listeners[type][callbackId];
			},

			broadcast(type, data, clone = true) {
				// Broadcast under a wildcard.
				{
					const wildCardCallbacks = listeners["*"];

					if (wildCardCallbacks) {
						Object.values(wildCardCallbacks).forEach((callback) => {
							try {
								if (clone) {
									callback(type.toLowerCase(), Object.assign({}, data));
								} else {
									callback(type.toLowerCase(), data);
								}
							} catch (e) {
								console.error("A listener produced an exception: ");
								console.error(e);
							}
						});
					}
				}

				// Broadcast under type.
				{
					const callbacks = listeners[type.toLowerCase()];

					if (callbacks) {
						Object.values(callbacks).forEach((callback) => {
							try {
								if (clone) {
									callback(Object.assign({}, data));
								} else {
									callback(data);
								}
							} catch (e) {
								console.error("A listener produced an exception: ");
								console.error(e);
							}
						});
					}
				}
			},

			destroy() {
				eventHandler.off("*", throwawyCallbackId);
			}
		};

		throwawyCallbackId = eventHandler.on("*", (type, data) => {
			instance.broadcast(type, data);
		});

		return instance;
	}


	// Setup the Bridge.
	const cefQuery = window.cefQuery;
	const eventHandler = new EventHandler();

	delete window.cefQuery;

	function onFailure(code, message) {
		console.error(`Javascript bridge query error:\ncode: ${code}\n\n${message}`)
	}

	function sendToParent(emission) {
		const payload = {
			type: "emission",
			data: emission
		};

		cefQuery({
			request: JSON.stringify(payload),
			persistent: false,
			onSuccess(response) { },
			onFailure: onFailure
		});
	}

	function sendQuery(field, nonce) {
		const payload = {
			type: "query",
			field: field,
			nonce: nonce
		};

		cefQuery({
			request: JSON.stringify(payload),
			persistent: false,
			onSuccess(response) { },
			onFailure: onFailure
		});
	}

	const Bridge = {
		emit(type, data = {}) {
			sendToParent({
				type: type,
				data: data
			});
		},

		query(field) {
			return new Promise((resolve) => {
				const nonce = `${Math.random()}${Math.random()}`.split(".").join("");

				eventHandler.once(`querynonce:${nonce}`, resolve);

				sendQuery(field, nonce);
			});
		},

		createThrowawayEventHandler() {
			return new ThrowawayEventHandler();
		},

		...eventHandler
	};

	Object.freeze(Bridge);
	Object.defineProperty(window, "Bridge", {
		value: Bridge,
		writable: false,
		configurable: false
	});
};