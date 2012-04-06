/* EncodeJs.groovy 
 *
 * A tool for obfuscating Javascript files, based on
 * a technique described by Brian Gosselin at
 * http://scriptasylum.com/tutorials/encode-decode.html
 *
 * This is not encryption, and it won't hide your code from
 * anyone educated, but it is probably good enough to keep
 * the average user from tampering with your script.
 */

static class EncodeJs
{
   static void main(args)
   {
      if (!(args && args.size() == 2)) {
         println "Usage: groovy EncodeJs source_file target_file"
         System.exit(-1)
      }

      File sourceFile = new File(args[0])
      File targetFile = new File(args[1])

      if (!sourceFile.exists()) {
         println "File ${args[0]} not found."
         System.exit(-1)
      }
      try {	
         targetFile.write(wrapCode(sourceFile.getText()))
         println "${targetFile.absolutePath} created."
      } catch (Throwable t) {
         println t
         System.exit(-1)
      }
   }

   /* A method to mimic Javascript's escape() function.
    * It replaces 'special characters' with their Unicode hexadecimal
    * equivalent. This simple method is not designed to handle 
    * double byte character sets. Feel free to improve it. 
    */
   static String escapeLikeJavascript(String sourceText) {
      def specialCharsRegex = /[^\w@*-+.\/]/
      return sourceText.replaceAll(specialCharsRegex, {
         "%${Integer.toHexString(it.codePointAt(0)).toUpperCase().padLeft(2, '0')}"
      })
   }

   /* Unlike the Javascript escape() function, this method replaces
    * ALL characters with their Unicode hexadecimal value, prefixed
    * with a '%'. 
    */
   static String encode(String sourceText) {
      StringBuffer encoded = new StringBuffer()
      for (int i=0; i < sourceText.length(); i++) {
         encoded.append("%${Integer.toHexString(sourceText.codePointAt(i))}")
      }
      return encoded.toString()
   }

   /* This method further obfuscates text by processing it as follows:
    * 1. Escapes all the special characters in the text
    * 2. Finds the Unicode values for each character in the resulting text
    * 3. Adds a constant number to each Unicode value to offset the character
    * 4. Derives new characters based on the offset Unicode values
    * 5. Appends the offset number to the end of the text so that the decoder
    *    can use it to reverse the process.
    * 6. Escapes special characters in the resulting text one more time. 
    */
   static String doubleEncode(String sourceText) {
      final int OFFSET = 7
      String payload = "<script type='text/javascript' charset='utf-8'>${sourceText}</script>"
      String escaped = this.escapeLikeJavascript(payload)
      StringBuffer encoded = new StringBuffer()
      for (int i=0; i < escaped.length(); i++) {
         encoded.append((char) (escaped.codePointAt(i) + OFFSET))
      }
      encoded.append(OFFSET)
      return this.escapeLikeJavascript(encoded.toString())
   }

   /* Wraps the double-encoded Javascript in a script that will decode it at runtime.
    */
   static String wrapCode(String sourceText) {
      String decoderFunction = """<script type='text/javascript' charset='utf-8'>function dF(s){ var s1=unescape(s.substr(0,s.length-1)); var t=''; for(i=0;i<s1.length;i++)t+=String.fromCharCode(s1.charCodeAt(i)-s.substr(s.length-1,1)); document.write(unescape(t)); }</script>"""
      return "document.write(unescape('${encode(decoderFunction)}')); dF('${doubleEncode(sourceText)}');"
   }

}
