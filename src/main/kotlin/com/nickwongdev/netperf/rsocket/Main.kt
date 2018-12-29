import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import io.rsocket.kotlin.*
import io.rsocket.kotlin.transport.ServerTransport
import io.rsocket.kotlin.transport.netty.server.NettyContextCloseable
import io.rsocket.kotlin.transport.netty.server.TcpServerTransport
import io.rsocket.kotlin.util.AbstractRSocket
import org.reactivestreams.Publisher
import java.net.InetSocketAddress

fun main(args: Array<String>) {

	val errors = Errors()
	val address = InetSocketAddress.createUnresolved("localhost", 9999)
	val serverTransport: ServerTransport<NettyContextCloseable> = TcpServerTransport.create(address.port)
	val serverAcceptor = ServerAcceptor()
	val server = RSocketFactory
		.receive()
		.errorConsumer(errors.errorsConsumer())
		.acceptor { serverAcceptor }
		.transport(serverTransport)
		.start()
		.blockingGet()

	server.onClose().blockingAwait()
}

internal class Errors {

	private val errors = ArrayList<Throwable>()

	fun errorsConsumer(): (Throwable) -> Unit = {
		println("Error consumed!")
		errors += (it)
	}

	fun errors() = errors
}

internal class ServerAcceptor
	: (Setup, RSocket) -> Single<RSocket> {

	private val serverHandlerReady = BehaviorProcessor.create<TestRSocketHandler>()

	override fun invoke(setup: Setup, sendingSocket: RSocket): Single<RSocket> {
		val handler = TestRSocketHandler(sendingSocket)
		serverHandlerReady.onNext(handler)
		return Single.just(handler)
	}

	fun handler(): Single<TestRSocketHandler> {
		return serverHandlerReady.firstOrError()
	}
}

internal class TestRSocketHandler(private val requester: RSocket? = null) : AbstractRSocket() {
	private val fnf = ArrayList<Data>()
	private val metadata = ArrayList<String>()

	override fun fireAndForget(payload: Payload): Completable {
		fnf += Data(payload)
		return Completable.complete()
	}

	override fun metadataPush(payload: Payload): Completable {
		metadata += payload.metadataUtf8
		return Completable.complete()
	}

	override fun requestResponse(payload: Payload): Single<Payload> {
		return Single.just(payload)
	}

	override fun requestStream(payload: Payload): Flowable<Payload> {
		return Flowable.just(payload)
	}

	override fun requestChannel(payloads: Publisher<Payload>): Flowable<Payload> {
		return Flowable.fromPublisher(payloads)
	}

	fun sendMetadataPush(payload: Payload): Completable = requester?.metadataPush(payload) ?: Completable.complete()
	fun fireAndForgetData() = fnf
	fun metadataPushData() = metadata
}

internal data class Data(val data: String, val metadata: String) {
	constructor(payload: Payload) : this(payload.dataUtf8, payload.metadataUtf8)

	fun payload(): Payload = DefaultPayload(data, metadata)
}