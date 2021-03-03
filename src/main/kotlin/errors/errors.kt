package errors
import java.lang.Exception


class AlreadyExistFileException : Exception()
class DbIntegrityViolationException : Exception()
class CantConvertGrpcArgsException : Exception()
class NotExistException : Exception()
class NotMatchingHashException : Exception()