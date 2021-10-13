package br.com.zup.edu.casadocodigo.autores

import br.com.zup.edu.casadocodigo.CasadocodigoGrpcKotlinServiceGrpc
import br.com.zup.edu.casadocodigo.NovoAutorRequest
import br.com.zup.edu.casadocodigo.NovoAutorResponse
import com.google.protobuf.Timestamp
import com.google.rpc.BadRequest
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId
import javax.validation.ConstraintViolationException

@Singleton
class NovoAutorEndpoint(@Inject val repository: AutorRepository) : CasadocodigoGrpcKotlinServiceGrpc.CasadocodigoGrpcKotlinServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    override fun cadastra(request: NovoAutorRequest, responseObserver: StreamObserver<NovoAutorResponse>) {

        LOGGER.info("request=$request")

        if (repository.existsByEmail(request.email)) {
            responseObserver.onError(Status.ALREADY_EXISTS
                            .withDescription("autor ja existente")
                            .asRuntimeException())
            return // nao esqueça de parar o fluxo de execucao
        }

        val autor = try {
            repository.save(request.toModel()) // retorna instancia MANAGED de Autor
        } catch (e: ConstraintViolationException) {
            e.printStackTrace()
            responseObserver.onError(handleConstraintValidationException(e))
            return // nao esqueça de parar o fluxo de execucao
        }

        with(responseObserver) {
            this.onNext(NovoAutorResponse.newBuilder()
                        .setId(autor.id!!)
                        .setCriadoEm(autor.criadoEm.toGrpcTimestamp())
                        .build())
            this.onCompleted() // finaliza request
        }
    }

    /**
     * Extension Functions
     */
    private fun NovoAutorRequest.toModel(): Autor {
        return Autor(
            nome = this.nome,
            email = this.email,
            descricao = this.descricao
        )
    }

    private fun LocalDateTime.toGrpcTimestamp(): Timestamp {
        val instant = this.atZone(ZoneId.of("UTC")).toInstant()
        return Timestamp.newBuilder()
                        .setSeconds(instant.epochSecond)
                        .setNanos(instant.nano)
                        .build()
    }

    /**
     * Converte lista de violations da ConstraintViolationException em mensagem BadRequest do gRPC
     */
    private fun handleConstraintValidationException(e: ConstraintViolationException): StatusRuntimeException {

        val violations = e.constraintViolations.map { // converte erros da Bean Validation em erros gRPC
            BadRequest.FieldViolation.newBuilder()
                .setField(it.propertyPath.last().name) // propertyPath=save.entity.email, mas quero somente "email"
                .setDescription(it.message)
                .build()
        }

        val badRequest = BadRequest.newBuilder() // com.google.rpc.BadRequest
            .addAllFieldViolations(violations)
            .build()

        val statusProto = com.google.rpc.Status.newBuilder()
            .setCode(Code.INVALID_ARGUMENT_VALUE)
            .setMessage("parametros de entrada invalidos")
            .addDetails(com.google.protobuf.Any.pack(badRequest)) // com.google.protobuf.Any
            .build()

        LOGGER.info("$statusProto")
        return StatusProto.toStatusRuntimeException(statusProto) // io.grpc.protobuf.StatusProto
    }

}