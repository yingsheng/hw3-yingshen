package edu.cmu.deiis.annotator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import edu.cmu.deiis.types.Question;

/**  
* QuestionAnnotator.java - identify question sentence after the "Q " and end with "?", 
* example: input - "Q Booth shot Lincoln?" annotated region: "Booth shot Lincoln?"
* the Question(type) annotation will be added to the JCas of the file
* @author  Ying Sheng
* @version 1.0 
*/ 

public class QuestionAnnotator extends JCasAnnotator_ImplBase {
  //create regular expression pattern for question
  private Pattern questionPattern=
          Pattern.compile("(?<=\\b[Q]\\s)(.+)[?]");
  @Override
  
  /**  
   * Take JCas as input, add new annotations to JCas
   * The JCas object is the data object inside UIMA where all the information is stored. 
   * It contains all annotations created by previous annotators, and the document text to be analyzed.   
   */
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    //get document text from JCas
    String docText=aJCas.getDocumentText();
    
    //match the question pattern
    Matcher matcher = questionPattern.matcher(docText);
    int pos=0;
 
    if (matcher.find(pos)) {
      //each document only has one question, set an question annotation
      Question annotation = new Question(aJCas);
      annotation.setBegin(matcher.start());
      annotation.setEnd(matcher.end());
      annotation.setConfidence(1.0);
      annotation.setCasProcessorId("QuestionAnnotator");
      
      //add the annotation to index
      annotation.addToIndexes();
    }
  }

} 





