package tschema

package object swagger {
  type SwaggerDescription = String

  type StatusCode = Int
  type MediaType  = String

  implicit class SwaggerOps[x](x: => x) {
    def mkSwagger(implicit swagger: MkSwagger[x]): MkSwagger[x]            = swagger
    def swaggerMapper(implicit mapper: SwaggerMapper[x]): SwaggerMapper[x] = mapper
  }

}
