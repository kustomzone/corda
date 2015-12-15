/*
 * Copyright 2015 Distributed Ledger Group LLC.  Distributed as Licensed Company IP to DLG Group Members
 * pursuant to the August 7, 2015 Advisory Services Agreement and subject to the Company IP License terms
 * set forth therein.
 *
 * All other rights reserved.
 */

package core

import core.messaging.MessagingService
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.time.Instant

/**
 * This file defines various 'services' which are not currently fleshed out. A service is a module that provides
 * immutable snapshots of data that may be changing in response to user or network events.
 */

/**
 * A wallet (name may be temporary) wraps a set of states that are useful for us to keep track of, for instance,
 * because we own them. This class represents an immutable, stable state of a wallet: it is guaranteed not to
 * change out from underneath you, even though the canonical currently-best-known wallet may change as we learn
 * about new transactions from our peers and generate new transactions that consume states ourselves.
 */
data class Wallet(val states: List<StateAndRef<OwnableState>>) {
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : OwnableState> statesOfType() = states.filter { it.state is T } as List<StateAndRef<T>>
}

/**
 * A [WalletService] is responsible for securely and safely persisting the current state of a wallet to storage. The
 * wallet service vends immutable snapshots of the current wallet for working with: if you build a transaction based
 * on a wallet that isn't current, be aware that it may end up being invalid if the states that were used have been
 * consumed by someone else first!
 */
interface WalletService {
    /**
     * Returns a read-only snapshot of the wallet at the time the call is made. Note that if you consume states or
     * keys in this wallet, you must inform the wallet service so it can update its internal state.
     */
    val currentWallet: Wallet
}

/**
 * The KMS is responsible for storing and using private keys to sign things. An implementation of this may, for example,
 * call out to a hardware security module that enforces various auditing and frequency-of-use requirements.
 *
 * The current interface is obviously not usable for those use cases: this is just where we'd put a real signing
 * interface if/when one is developed.
 */
interface KeyManagementService {
    val keys: Map<PublicKey, PrivateKey>

    fun toPrivate(publicKey: PublicKey) = keys[publicKey] ?: throw IllegalStateException("No private key known for requested public key")

    /** Generates a new random key and adds it to the exposed map. */
    fun freshKey(): KeyPair
}

/**
 * An identity service maintains an bidirectional map of [Party]s to their associated public keys and thus supports
 * lookup of a party given its key. This is obviously very incomplete and does not reflect everything a real identity
 * service would provide.
 */
interface IdentityService {
    fun partyFromKey(key: PublicKey): Party?
}

/**
 * Simple interface (for testing) to an abstract timestamping service, in the style of RFC 3161. Note that this is not
 * 'timestamping' in the block chain sense, but rather, implies a semi-trusted third party taking a reading of the
 * current time, typically from an atomic clock, and then digitally signing (current time, hash) to produce a timestamp
 * triple (signature, time, hash). The purpose of these timestamps is to locate a transaction in the timeline, which is
 * important in the absence of blocks. Here we model the timestamp as an opaque byte array.
 */
interface TimestamperService {
    fun timestamp(hash: SecureHash): ByteArray
    fun verifyTimestamp(hash: SecureHash, signedTimestamp: ByteArray): Instant
}

/**
 * A sketch of an interface to a simple key/value storage system. Intended for persistence of simple blobs like
 * transactions, serialised protocol state machines and so on. Again, this isn't intended to imply lack of SQL or
 * anything like that, this interface is only big enough to support the prototyping work.
 */
interface StorageService {
    fun <K,V> getMap(tableName: String): MutableMap<K, V>
}

/**
 * A service hub simply vends references to the other services a node has. Some of those services may be missing or
 * mocked out. This class is useful to pass to chunks of pluggable code that might have need of many different kinds of
 * functionality and you don't want to hard-code which types in the interface.
 */
interface ServiceHub {
    val walletService: WalletService
    val keyManagementService: KeyManagementService
    val identityService: IdentityService
    val timestampingService: TimestamperService
    val storageService: StorageService
    val networkService: MessagingService
}