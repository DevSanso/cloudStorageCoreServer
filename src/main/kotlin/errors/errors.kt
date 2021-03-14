package errors
import java.lang.Exception


open class AlreadyExistException : Exception()
class AlreadyExistFileException : AlreadyExistException()
class DbIntegrityViolationException : Exception()
class CantConvertGrpcArgsException : Exception()
class NotExistException : Exception()
class NotMatchingHashException : Exception()
class ViolationOfAccessException : Exception()
class OverflowException : ArrayIndexOutOfBoundsException()