export default function formatKey(key: string, keyType: string) {
	let prefix =
		keyType === 'public'
			? '-----BEGIN PUBLIC KEY-----\n'
			: '-----BEGIN RSA PRIVATE KEY-----\n'
	let suffix =
		keyType === 'public'
			? '\n-----END PUBLIC KEY-----\n'
			: '\n-----END RSA PRIVATE KEY-----\n'

	if (key) {
		const lines = key.split('\n')
		lines.shift()
		lines.pop()
		lines.pop()
		return prefix + lines.join('') + suffix
	} else {
		throw new Error(
			'There was an error while formating the auto generated keys!'
		)
	}
}
