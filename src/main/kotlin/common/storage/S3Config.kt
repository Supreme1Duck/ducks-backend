package com.ducks.common.storage

data class S3Config(
    val endpoint: String,
    val region: String,
    val bucket: String,
    val accessKey: String,
    val secretKey: String,
    // Отдельно от [endpoint]: если сверху появится CDN или свой домен, поменяется
    // только адрес раздачи, а перезаливать объекты и переписывать ссылки не придётся.
    val publicBaseUrl: String,
    val publicReadAcl: Boolean,
)
