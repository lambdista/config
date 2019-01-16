/*
 * Copyright 2016 Alessandro Lacava.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import sbt.Keys._
import sbt._

object Publishing extends SonatypePublishing() {
  val projectUrl = "https://github.com/lambdista/config"
  val developerId = "lambdista"
  val developerName = "Alessandro Lacava"
  val developerUrl = "http://www.alessandrolacava.com"
  val licenseName = "Apache License, Version 2.0"
  val licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.txt"
}

abstract class SonatypePublishing() {
  def projectUrl: String
  def developerId: String
  def developerName: String
  def developerUrl: String
  def licenseName: String
  def licenseUrl: String
  def scmUrl = projectUrl
  def scmConnection = "scm:git:" + scmUrl

  def settings: Seq[Setting[_]] = Seq(
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { _ => false },
    licenses := Seq(licenseName -> url(licenseUrl)),
    homepage := Some(url(projectUrl)),
    autoAPIMappings := true,
    apiURL := Some(url("https://lambdista.github.io/config/api/")),
    pomExtra := (
      <scm>
        <url>{scmUrl}</url>
        <connection>{scmConnection}</connection>
      </scm>
        <developers>
          <developer>
            <id>{developerId}</id>
            <name>{developerName}</name>
            <url>{developerUrl}</url>
          </developer>
        </developers>),
    publishArtifact in Test := false,
    pomIncludeRepository := (_ => false)
  )
}
