package edu.cmu.deiis.annotator;

import java.util.Arrays;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import edu.cmu.deiis.types.*;

/**  
* NGramAnnotator.java - identify 1- 2- 3-Gram based on token annotation in each ranges annotated by question or answer
* example: input - "Booth shot Lincoln?"(annotated as question) 
* annotation:  "Booth" "shot" "Lincoln" "Booth shot" "shot Lincoln" "Booth shot Lincoln"
* the elements in NGram is an FSArray consisting of Token annotations.  
* the NGram(type) annotation will be added to the JCas of the file
* @author  Ying Sheng
* @version 1.0 
*/ 

public class NGramAnnotator extends JCasAnnotator_ImplBase {
  /**  
   * Take JCas as input, add new annotations to JCas
   * The JCas object is the data object inside UIMA where all the information is stored. 
   * It contains all annotations created by previous annotators, and the document text to be analyzed.   
   */
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    // TODO Auto-generated method stub
    
    FSIterator<org.apache.uima.jcas.tcas.Annotation> QueIterator = aJCas.getAnnotationIndex(Question.type).iterator();
    FSIterator<org.apache.uima.jcas.tcas.Annotation> AnsIterator = aJCas.getAnnotationIndex(Answer.type).iterator();
    
    int QAnum=1+aJCas.getAnnotationIndex(Answer.type).size();
    int[] QABegin=new int[QAnum];
    int[] QAEnd=new int[QAnum]; 
    
    //get the begin/end position of question annotation
    while(QueIterator.hasNext()){      
      Question QueAnnotation=(Question) QueIterator.next(); 
      QABegin[0]=QueAnnotation.getBegin();
      QAEnd[0]=QueAnnotation.getEnd();      
    }
    
    //get the begin/end position for each answer annotation    
    int count=1;
    while(AnsIterator.hasNext()){
      //find the question annotation
      Answer AnsAnnotation=(Answer) AnsIterator.next(); 
      QABegin[count]=AnsAnnotation.getBegin();
      QAEnd[count]=AnsAnnotation.getEnd();
      count++;
    }
    
    
    //go through all the token annotations
    FSIterator<org.apache.uima.jcas.tcas.Annotation> TokIterator = aJCas.getAnnotationIndex(Token.type).iterator();    
    int TokenNum=aJCas.getAnnotationIndex(Token.type).size();
    
    FSArray tokenList=new FSArray(aJCas, TokenNum);
    
    //add token to tokenlist, generate all the 1-Gram
    
    int[] QAtokenNum=new int[QAnum]; //initialized to 0  
    int i=0;
    
    count=0; //count the count-th token
    while(TokIterator.hasNext()){
      Token TokAnnotation=(Token)TokIterator.next();
      tokenList.set(count,TokAnnotation);
      //each token is stored in a 1-Gram annotator
      NGram annotation=new NGram(aJCas);
      annotation.setConfidence(1.0);
      annotation.setCasProcessorId("NGramAnnotator");          
      annotation.setBegin(TokAnnotation.getBegin());
      annotation.setEnd(TokAnnotation.getEnd());
      
      FSArray elements=new FSArray(aJCas, 1);
      elements.set(0, TokAnnotation);
      annotation.setElements(elements);
      
      annotation.setElementType("edu.cmu.deiis.type.Token");
      annotation.addToIndexes();
      if (TokAnnotation.getEnd()>QAEnd[i]) {
        QAtokenNum[i]=count; 
        i++;
      }     
      count++;             
    }
     QAtokenNum[i]=TokenNum;
    
    //start to generate 2-Gram and 3-Gram
     int firstTokenIdx, lastTokenIdx, currentTokNum;
     for (i=0;i<QAnum;i++){
       //in each sentence, the number of tokens is QAtokenNum[i]-QAtokenNum[i-1]
       if (i==0) {firstTokenIdx=0;}
       else {firstTokenIdx=QAtokenNum[i-1];}
       lastTokenIdx=QAtokenNum[i]-1;
       
       currentTokNum=lastTokenIdx-firstTokenIdx+1;
       //2-Gram
       for (int j=firstTokenIdx; j<lastTokenIdx; j++){
         NGram annotation=new NGram(aJCas);
         
         Token token0=(Token)tokenList.get(j);
         Token token1=(Token)tokenList.get(j+1);
         
         FSArray elements=new FSArray(aJCas, 2);
         elements.set(0, token0);
         elements.set(1, token1);
         annotation.setElements(elements);
         
         annotation.setBegin(token0.getBegin());
         annotation.setEnd(token1.getEnd());
         
         annotation.setElementType("edu.cmu.deiis.type.Token");
         annotation.setConfidence(1.0);
         annotation.setCasProcessorId("NGramAnnotator");
         
         annotation.addToIndexes();
         
       }
       
       //3-Gram
       if (currentTokNum>=3) {
         for (int j=firstTokenIdx; j<lastTokenIdx-1; j++){
           NGram annotation=new NGram(aJCas);
           
           Token token0=(Token)tokenList.get(j);
           Token token1=(Token)tokenList.get(j+1);
           Token token2=(Token)tokenList.get(j+2);
           FSArray elements=new FSArray(aJCas, 3);
           elements.set(0, token0);
           elements.set(1, token1);
           elements.set(2, token2);
           annotation.setElements(elements);
           
           annotation.setBegin(token0.getBegin());
           annotation.setEnd(token2.getEnd());
           
           annotation.setElementType("edu.cmu.deiis.type.Token");
           annotation.setConfidence(1.0);
           annotation.setCasProcessorId("NGramAnnotator");
           
           annotation.addToIndexes();
           
         }
       
       }  
       
     }
     
  }

}

