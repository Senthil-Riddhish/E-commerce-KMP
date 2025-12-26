package org.example.project

import Data.Product
import apiClient.httpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class HomeRepository {
    suspend fun getProductsApi(): List<Product> {
        val response = httpClient.get("https://fakestoreapi.com/products")
        return response.body()
    }
    
    fun getProducts(): Flow<List<Product>> = flow {
        emit(getProductsApi())
    }
}