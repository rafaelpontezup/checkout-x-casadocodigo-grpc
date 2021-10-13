package br.com.zup.edu.casadocodigo.autores

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Entity
class Autor(
    @field:NotBlank
    @Column(nullable = false)
    val nome: String,

    @field:Email
    @field:NotBlank
    @Column(unique = true, nullable = false)
    val email: String,

    @field:NotBlank
    @field:Size(max = 400)
    @Column(nullable = false, length = 400)
    val descricao: String
) {

    @Id
    @GeneratedValue
    var id: Long? = null

    @Column(nullable = false, updatable = false)
    val criadoEm: LocalDateTime = LocalDateTime.now();

}