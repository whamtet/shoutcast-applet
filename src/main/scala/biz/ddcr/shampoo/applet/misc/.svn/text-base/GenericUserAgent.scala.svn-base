/*
 *  Copyright (C) 2012 okay_awright <okay_awright AT ddcr DOT biz>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package biz.ddcr.shampoo.applet.misc

/**
 * Generates a User Agent-like text identifier, from the components used by the
 * applet to identify it, following this syntax: <getCompatibilityClient()>
 * (<getJavaIdentifier()>; ) Applet_ShampooGeneric (<getModules()>)
 */

trait GenericUserAgent extends Serializable {

  class VersionizedComponent(private val _component: Option[String], private val _version: Option[String] = None) extends Serializable {

    def component: Option[String] = _component
    def version: Option[String] = _version

    override def toString: String = {
      (component.getOrElse("") :: version.getOrElse("") :: Nil).filter(!_.isEmpty).mkString("/")
    }
  }

  protected def modules: Option[Iterable[Option[VersionizedComponent]]]
  protected def compatibilityClient: Option[VersionizedComponent]

  protected def javaIdentifier: Option[VersionizedComponent] = {
    Option(new VersionizedComponent(
        Some("Java"),
        Option(System.getProperty("java.version"))))
  }

  protected def platformIdentifier: Option[VersionizedComponent] = {
    Option(new VersionizedComponent(
        Option(System.getProperty("os.name")),
        Option(System.getProperty("os.version"))))
  }

  protected def shampooAppletIdentifier: Option[VersionizedComponent] = {
    Option(new VersionizedComponent(
        Some("ShoucastPlayerApplet2"),
        GenericUserAgent.classVersion(this.getClass)))
  }

  protected def architectureIdentifier: Option[VersionizedComponent] = {
    Option(new VersionizedComponent(
        Option(System.getProperty("os.arch"))))
  }

  lazy val banner: String = {

    val userAgent = (compatibilityClient.getOrElse("") :: " (" :: javaIdentifier.getOrElse("") :: "; " :: platformIdentifier.getOrElse("") :: "; " :: architectureIdentifier.getOrElse("") :: ") " :: shampooAppletIdentifier.getOrElse(this.getClass.getName) :: Nil).mkString
    if (modules.isDefined && !modules.isEmpty)
      (userAgent :: " (" :: (modules.filter(!_.isEmpty).mkString("; ")) :: ")" :: Nil).mkString
    userAgent
  }

  override def toString: String = banner
}

object GenericUserAgent {
  /*
   * Get the version of a class from its package getPackage() is known to only
   * work when working with the standard classloader
   */
  def classVersion(clazz: Class[_]): Option[String] = {
    //Java compatibility: can return null
    val packge: Option[Package] = Option(clazz.getPackage)
    packge match {
      case Some(packge) => {
          val implementationVersion = Option(packge.getImplementationVersion)
          implementationVersion match {
            case Some(implementationVersion) => Some(implementationVersion)
            //Fallback
            case None => Option(packge.getSpecificationVersion)
          }
        }
      case None => None
    }
  }
}
