package main
import scalatags.Text.all._
import scala.math.min
import mainargs.{main, ParserForMethods}
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory, Validator}
import org.xml.sax.{ErrorHandler, SAXParseException}
import java.net.URL 


object Main {

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      Console.err.println("Usage: mill main.run <schema.xsd> <file1.xml> [file2.xml ...]")
      sys.exit(2)
    }

    val exitCode = validate(args.tail, args.head)
    sys.exit(exitCode)

  }

  def validate(xmlFiles: Array[String], xsdFile: String): Int = {

    try {
      val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)

      val schemaUrl =
        new URL("file://" + System.getProperty("user.dir") + "/" + xsdFile)

      println("Schema: " + schemaUrl)

      val schema =
        schemaFactory.newSchema(new StreamSource(schemaUrl.openStream()))

      def validateFile(xmlFile: String): (String, List[String]) = {
        var exceptions = List[String]()

        try {
          val validator = schema.newValidator()

          validator.setErrorHandler(new ErrorHandler() {
            override def warning(exception: SAXParseException): Unit =
              exceptions = exception.getMessage :: exceptions

            override def fatalError(exception: SAXParseException): Unit =
              exceptions = exception.getMessage :: exceptions

            override def error(exception: SAXParseException): Unit =
              exceptions = exception.getMessage :: exceptions
          })

          val xmlUrl =
            new URL("file://" + System.getProperty("user.dir") + "/" + xmlFile)

          validator.validate(new StreamSource(xmlUrl.openStream()))

          (xmlFile, exceptions.reverse)

        } catch {
          case ex: Exception =>
            (xmlFile, (exceptions.reverse :+ ex.getMessage).distinct)
        }
      }

      val results = xmlFiles.map(validateFile)
      val failed = results.filter { case (_, messages) => messages.nonEmpty }

      if (failed.isEmpty) {
        println(s"All ${xmlFiles.length} files adhere to the schema.")
        0
      } else {
        println(s"${failed.length} of ${xmlFiles.length} files failed validation:\n")

        failed.foreach { case (file, messages) =>
          println(s"Validate: $file")
          messages.foreach(println)
          println("Number of exceptions: " + messages.length)
          println()
        }

        1
      }

    } catch {
      case ex: Exception =>
        println("Exception in the validation.")
        println("Exception message: " + ex.getMessage)
        2
    }
  }
}
