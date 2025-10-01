resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

// To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

addSbtPlugin("uk.gov.hmrc"        %  "sbt-auto-build"          % "3.24.0")
addSbtPlugin("uk.gov.hmrc"        %  "sbt-distributables"      % "2.6.0")
addSbtPlugin("org.playframework"  %  "sbt-plugin"              % "3.0.9")
addSbtPlugin("org.scoverage"      %  "sbt-scoverage"           % "2.3.1")
addSbtPlugin("com.timushev.sbt"   %  "sbt-updates"             % "0.6.4")

