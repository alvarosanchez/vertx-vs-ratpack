package com.alvarosanchez.teams.ratpack

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import ratpack.handling.Context
import ratpack.handling.Handler

import java.text.ParseException

class JwtAuthentication {

    static String secret = 'secret'*8

    static Handler authenticate() {
        return { Context ctx ->
            String token = ctx.request.headers.get('Authorization')?.substring('Bearer '.length())
            boolean valid = false
            if (token) {
                try {
                    SignedJWT jwt = SignedJWT.parse(token)
                    JWSVerifier verifier = new MACVerifier(secret)
                    valid = jwt.verify(verifier)
                } catch (ParseException pe){}
            }

            valid ? ctx.next() : ctx.response.status(401).send()
        }
    }

    static Handler login() {
        return { Context ctx ->
            JWSSigner signer = new MACSigner(secret)

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject('alvaro.sanchez')
                    .expirationTime(new Date(new Date().time + 60 * 1000))
                    .build()

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet)
            signedJWT.sign(signer)

            ctx.response.send(signedJWT.serialize())
        }
    }

}
