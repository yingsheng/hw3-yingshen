package edu.cmu.deiis.annotator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;

import edu.cmu.deiis.types.*;

/**  
* TokenAnnotator.java - identify tokens in the range annotated as Question type or Answer type.
* example: input - "Booth shot Lincoln?"(annotated as question) 
* annotation:  "Booth" "shot" "Lincoln"
* the Token(type) annotation will be added to the JCas of the file
* @author  Ying Sheng
* @version 1.0 
*/ 

public class TokenAnnotator extends JCasAnnotator_ImplBase {
  
  private Pattern tokenPattern=
         Pattern.compile("\\b\\w+\\b");
  
  @Override
  /**  
   * Take JCas as input, add new annotations to JCas
   * The JCas object is the data object inside UIMA where all the information is stored. 
   * It contains all annotations created by previous annotators, and the document text to be analyzed.   
   */
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    
    //get document text from JCas
    String docText=aJCas.getDocumentText();
    FSIterator<org.apache.uima.jcas.tcas.Annotation> QueIterator = aJCas.getAnnotationIndex(Question.type).iterator();
    
    while(QueIterator.hasNext()){
      //Answer annotation = new Answer(aJCas);
      Question QueAnnotation=(Question) QueIterator.next(); 
      int QueBegin=QueAnnotation.getBegin();
      int QueEnd=QueAnnotation.getEnd();
      String QueStr=docText.substring(QueBegin,QueEnd);
      
      //match token pattern
      Matcher matcherToken=tokenPattern.matcher(QueStr);
      int pos=0;
      
      while (matcherToken.find(pos)) {
        //add token annotation for answer
        Token annotation = new Token(aJCas);
        annotation.setBegin(matcherToken.start()+QueBegin);
        annotation.setEnd(matcherToken.end()+QueBegin);
        annotation.setConfidence(1.0);
        annotation.setCasProcessorId("TokenAnnotator");
        
        //add the annotation to index
        annotation.addToIndexes();
        pos=matcherToken.end();
      }      
    
    }
    
   
    FSIterator<org.apache.uima.jcas.tcas.Annotation> AnsIterator = aJCas.getAnnotationIndex(Answer.type).iterator();
//    Iterator<Annotation> typeIterator=aJCas.getAnnotationIndex(Answer.type);
 
     
    while(AnsIterator.hasNext()){
       //Answer annotation = new Answer(aJCas);
       Answer AnsAnnotation=(Answer) AnsIterator.next(); 
       int AnsBegin=AnsAnnotation.getBegin();
       int AnsEnd=AnsAnnotation.getEnd();
       String AnsStr=docText.substring(AnsBegin,AnsEnd);
       
       //match token pattern
       Matcher matcher=tokenPattern.matcher(AnsStr);
       int pos=0;
       
       while (matcher.find(pos)) {
         //add token annotation for answer
         Token annotation = new Token(aJCas);
         annotation.setBegin(matcher.start()+AnsBegin);
         annotation.setEnd(matcher.end()+AnsBegin);
         annotation.setConfidence(1.0);
         annotation.setCasProcessorId("TokenAnnotator");
         
         //add the annotation to index
         annotation.addToIndexes();
         pos=matcher.end();
       }
    }

  }

}
