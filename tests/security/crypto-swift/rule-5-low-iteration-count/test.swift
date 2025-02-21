import Foundation

// ********************* CODEQL-BASED TESTS *********************

// These tests are modified from https://github.com/github/codeql
// https://github.com/github/codeql/blob/main/swift/ql/test/query-tests/Security/CWE-916/test.swift
// Swift's analysis for CWE-916
// GOOD / BAD annotations are CodeQL's annotations

// We modified getRandomArray() to return AES.randomIV(10).
// The original implementation of getRandomArray() was the same as
// CryptoSwift's randomIV().

// --- stubs ---

// These stubs roughly follows the same structure as classes from CryptoSwift
// Most of these are from CodeQL's tests, but may be slightly modified to
// more accuratly match the real API

protocol Cryptors: AnyObject {
  static func randomIV(_ blockSize: Int) -> Array<UInt8>
}

extension Cryptors {
  static func randomIV(_ count: Int) -> Array<UInt8> {
    (0..<count).map({ _ in UInt8.random(in: 0...UInt8.max) })
  }
}

class AES: Cryptors { }

class HMAC: Cryptors
{
  enum Variant {
    case md5
    case sha1
    case sha2(SHA2.Variant)
    case sha3(SHA3.Variant)
  }
}

protocol DigestType {}

class SHA2: DigestType {
  enum Variant {
    case sha224, sha256, sha384, sha512
  }
}

class SHA3: DigestType {
  enum Variant {
    case sha224, sha256, sha384, sha512, keccak224, keccak256, keccak384, keccak512
  }
}

enum PKCS5 { }
extension PKCS5 {
  struct PBKDF1 {
	  init(password: Array<UInt8>, salt: Array<UInt8>, variant: Variant = .sha1, iterations: Int = 4096, keyLength: Int? = nil) { }
    enum Variant {
      case md5
      case sha1
      case sha2(SHA2.Variant)
      case sha3(SHA3.Variant)
    }
  }

  struct PBKDF2 {
	  init(password: Array<UInt8>, salt: Array<UInt8>, iterations: Int = 4096, keyLength: Int? = nil, variant: HMAC.Variant = HMAC.Variant.sha2(.sha256)) { }
  }
}

struct HKDF {
	init(password: Array<UInt8>, salt: Array<UInt8>? = nil, info: Array<UInt8>? = nil, keyLength: Int? = nil, variant: HMAC.Variant = HMAC.Variant.sha2(.sha256)) { }
}

final class Scrypt {
	init(password: Array<UInt8>, salt: Array<UInt8>, dkLen: Int, N: Int, r: Int, p: Int) { }
}


// Helper functions

func getRandomArray() -> Array<UInt8> {
	AES.randomIV(10)
}

func unknownCondition() -> Bool {
  Bool.random()
}

func getLowIterationCount() -> Int { return 99999 }

func getEnoughIterationCount() -> Int { return 120120 }

// --- tests ---

// Excluding added SWAN annotations and modifications to types (to more
// closely match the real API), this function has not been modified
// from CodeQL's tests.
func codeql_test() {
	let randomArray = getRandomArray()
	// let variant = Variant.sha2
	let lowIterations = getLowIterationCount()
	let enoughIterations = getEnoughIterationCount() 
	
	// PBKDF1 test cases
	let pbkdf1b1 = PKCS5.PBKDF1(password: randomArray, salt: randomArray, iterations: lowIterations, keyLength: 0) // BAD //$ITERATION$error
	let pbkdf1b2 = PKCS5.PBKDF1(password: randomArray, salt: randomArray, iterations: 80000, keyLength: 0) // BAD //$ITERATION$error
	let pbkdf1g1 = PKCS5.PBKDF1(password: randomArray, salt: randomArray, iterations: enoughIterations, keyLength: 0) // GOOD
	let pbkdf1g2 = PKCS5.PBKDF1(password: randomArray, salt: randomArray, iterations: 120120, keyLength: 0) // GOOD

	// PBKDF2 test cases
	let pbkdf2b1 = PKCS5.PBKDF2(password: randomArray, salt: randomArray, iterations: lowIterations, keyLength: 0) // BAD //$ITERATION$error
	let pbkdf2b2 = PKCS5.PBKDF2(password: randomArray, salt: randomArray, iterations: 80000, keyLength: 0) // BAD //$ITERATION$error
	let pbkdf2g1 = PKCS5.PBKDF2(password: randomArray, salt: randomArray, iterations: enoughIterations, keyLength: 0) // GOOD
	let pbkdf2g2 = PKCS5.PBKDF2(password: randomArray, salt: randomArray, iterations: 120120, keyLength: 0) // GOOD
}

// ********************* SWAN TESTS *********************

func test_r5_simple_violation() throws {
  let iterations = getLowIterationCount()
  let randomArray = getRandomArray()
  
  // Violation SHOULD be detected only for iterations argument
  _ = try PKCS5.PBKDF1(password: randomArray, salt: randomArray, iterations: iterations, keyLength: 0) //$ITERATION$error
  _ = try PKCS5.PBKDF2(password: randomArray, salt: randomArray, iterations: iterations, keyLength: 0) //$ITERATION$error
}

func test_r5_multiple_values_violation() throws {
  let iterations = unknownCondition() ? getLowIterationCount() : getEnoughIterationCount()
  let randomArray = getRandomArray()
    
  // Violation SHOULD be detected only for iterations argument
  _ = try PKCS5.PBKDF1(password: randomArray, salt: randomArray, iterations: iterations, keyLength: 0) //$ITERATION$error
}

func test_r5_simple_no_violation() throws {
  let iterations = getEnoughIterationCount()
  let randomArray = getRandomArray()
  
  // Violation SHOULD NOT be detected
  _ = try PKCS5.PBKDF1(password: randomArray, salt: randomArray, iterations: iterations, keyLength: 0)
  _ = try PKCS5.PBKDF2(password: randomArray, salt: randomArray, iterations: iterations, keyLength: 0)
}
