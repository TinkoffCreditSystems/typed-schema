name := "typed-schema-macros"

libraryDependencies += scalaOrganization.value % "scala-reflect" % scalaVersion.value
libraryDependencies += "com.chuusai" %% "shapeless" % Version.shapeless
libraryDependencies += "org.typelevel" %% "cats-core" % Version.cats

addCompilerPlugin("org.typelevel" %% "kind-projector" % Version.kindProjector)