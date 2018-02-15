package com.campspot.jdbi3

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class InTransaction(val name: String)
