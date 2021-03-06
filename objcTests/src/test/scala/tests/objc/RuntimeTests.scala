package tests.objc

import utest._
import scalanative.native._
import objc._
import runtime._

object RuntimeTests extends TestSuite {

  def inc(self: id, sel: SEL, i: Int): Int = i+1

  // due to a bug in scala-native, we can't define ObjC methods in the TestSuite
  // (last checked with SN 0.3.8)
  def defineFoo(cls: ClassPtr): Boolean = {
    val sel = sel_registerName(c"inc:")
    class_addMethod(cls,sel,CFunctionPtr.fromFunction3(inc),c"i@:i")
  }

  val tests = Tests{

    'objc_getClass-{
      objc_getClass(c"foo") ==> objc_Nil

      assert( objc_getClass(c"NSObject") != objc_Nil )
    }
    'class_getName-{
      val cls = objc_getClass(c"NSObject")

      fromCString( class_getName(objc_Nil) ) ==> "nil"
      fromCString( class_getName(cls) ) ==> "NSObject"
    }
    'class_createInstance__object_getClass-{
      val cls = objc_getClass(c"NSObject")
      val obj = class_createInstance(cls,0)
      assert( obj != objc_nil )

      val cls2 = object_getClass(obj)
      fromCString( class_getName( cls2 ) ) ==> "NSObject"
    }
    'createClass-{
      val cls = objc_allocateClassPair(objc_getClass(c"NSObject"),c"MyClass",0)
      fromCString( class_getName(cls) ) ==> "MyClass"

      val outCount = stackalloc[UInt]
      var methods = class_copyMethodList(cls,outCount)
      !outCount ==> 0.toUInt
      stdlib.free(methods.cast[Ptr[Byte]])

      val metaClass = object_getClass(cls)
      fromCString( class_getName(metaClass) ) ==> "MyClass"

      // add instance methods
      defineFoo(cls) ==> true
      methods = class_copyMethodList(cls,outCount)
      !outCount ==> 1.toUInt
      stdlib.free(methods.cast[Ptr[Byte]])

      // add instance var
      class_addIvar(cls,c"var",sizeof[Int],2.toUByte,c"i") ==> true
      var ivars = class_copyIvarList(cls,outCount)
      !outCount ==> 1.toUInt
      stdlib.free(ivars.cast[Ptr[Byte]])

      objc_registerClassPair(cls)
      fromCString( class_getName( objc_getClass(c"MyClass") ) ) ==> "MyClass"
    }
    'msgSend-{
      val cls = objc_getClass(c"MyClass")
      fromCString( class_getName(cls) ) ==> "MyClass"

      val selAlloc = sel_registerName(c"alloc")
      val selInit = sel_registerName(c"init")
      val inst = objc_msgSend(cls,selAlloc)
      assert( inst != null )
      objc_msgSend(inst,selInit)

      val sel = sel_registerName(c"inc:")
      objc_msgSend(inst,sel,1).cast[Int] ==> 2
    }
  }

}

