package main
import scalatags.Text.all._
import scala.math.min
import mainargs.{main, ParserForMethods}
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory, Validator}
import org.xml.sax.{ErrorHandler, SAXParseException}
import java.net.URL 

object ValidationResult extends Enumeration {
  type ValidationResult = Value
  val Pass, Fail, ValidationError = Value
}

object Main {

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      Console.err.println("Usage: mill main.run <schema.xsd> <file1.xml> [file2.xml ...]")
      sys.exit(2)
    }

    val validationResult = validate(args.tail, args.head)
    validationResult match {
      case ValidationResult.Pass => sys.exit(0)
      case ValidationResult.Fail => sys.exit(1)
      case _ => sys.exit(2)
    }

  }

  def validate(xmlFiles: Array[String], xsdFile: String): ValidationResult.ValidationResult = {

    try {
      val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)

      val userDir = System.getProperty("user.dir")
      val schemaUrl = new URL("file://" + userDir + "/" + xsdFile)

      println("Schema: " + schemaUrl)

      val schema =
        schemaFactory.newSchema(new StreamSource(schemaUrl.openStream()))

      def validateFile(xmlFile: String): (String, List[String]) = {
        var exceptions = List[String]()
        try {
          val validator = schema.newValidator()
          validator.setErrorHandler(new ErrorHandler() {
            @Override
            def warning(exception: SAXParseException) = exceptions = exception.getMessage :: exceptions
            @Override
            def fatalError(exception: SAXParseException) = exceptions = exception.getMessage :: exceptions
            @Override
            def error(exception: SAXParseException) = exceptions = exception.getMessage :: exceptions
          })
          val xmlUrl = new URL("file://" + userDir + "/" + xmlFile)
          validator.validate(new StreamSource(xmlUrl.openStream()))
          (xmlFile, exceptions.reverse)
        } catch {
          case ex: Exception =>
            (xmlFile, (exceptions.reverse :+ ex.getMessage).distinct)
        }
      }

      val results = xmlFiles.map(validateFile)
      val failed = results.filter( _._2.nonEmpty )

      if (failed.isEmpty) {
        println(s"All ${xmlFiles.length} file(s) adhere to the schema $xsdFile.")
        ValidationResult.Pass
      } else {
        println(s"${failed.length} of ${xmlFiles.length} file(s) failed validation:\n")
        failed.foreach { case (file, messages) =>
          Console.err.println(s"Validate: $file")
          messages.foreach(Console.err.println)
          Console.err.println(s"Number of exceptions in $file: " + messages.length)
          Console.err.println()
        }
        ValidationResult.Fail
      }
    // If the validation itself fails
    } catch {
      case ex: Exception =>
        Console.err.println("Error: unable to run the validation due to the following exception:")
        Console.err.println(ex.getMessage)
        Console.err.println()
        ValidationResult.ValidationError
    }
  }
}
