

package edu.cmu.deiis.annotator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.TOP;
import org.cleartk.ne.type.NamedEntity;
import org.cleartk.ne.type.NamedEntityMention;

import edu.cmu.deiis.types.*;

/**  
* AnswerScoreAnnotator.java - calculate scores for each answer by (NGram overlapped with question)/(total NGram number) 
* the AnswerScore(type) annotation will be added to the JCas of the file
* @author  Ying Sheng
* @version 1.0 
*/ 

public class AnswerScoreAnnotator extends JCasAnnotator_ImplBase {
  /**  
   * Take JCas as input, add new annotations to JCas
   * The JCas object is the data object inside UIMA where all the information is stored. 
   * It contains all annotations created by previous annotators, and the document text to be analyzed.   
   */
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    
    String docText=aJCas.getDocumentText();
    
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
    
    
    FSIterator<org.apache.uima.jcas.tcas.Annotation> NGIterator = aJCas.getAnnotationIndex(NGram.type).iterator();

    //find the NGram in question
    ArrayList<String> QueNGramStr = new ArrayList<String>();
    
    int beginpos,endpos;
    String NGramString;
    while(NGIterator.hasNext()){
      NGram NGAnnotation=(NGram) NGIterator.next();
      beginpos=NGAnnotation.getBegin();
      endpos=NGAnnotation.getEnd();
      if (beginpos<QABegin[1]){ //if the end of the NGram is before the start of the first answer, then the NGram is in question
        NGramString=docText.substring(beginpos,endpos);
        QueNGramStr.add(NGramString);
      } 
      else {break;}
    }
    
    //for each answer, find the overlap of NGram with answer
    FSIterator<org.apache.uima.jcas.tcas.Annotation> NGIterator2 = aJCas.getAnnotationIndex(NGram.type).iterator();
    int[] overlapNum=new int[QAnum-1]; //the # of NGrams that overlap with question in the answer
    int[] totalNum=new int[QAnum-1]; // the total # of NGrams in the answer
    
    int AnsIdx=0;
    boolean found;
    while(NGIterator2.hasNext()){
      NGram NGAnnotation=(NGram) NGIterator2.next();
      beginpos=NGAnnotation.getBegin();
      endpos=NGAnnotation.getEnd();
      if (beginpos>QAEnd[AnsIdx]){ //if the end of the NGram is before the start of the first answer, then the NGram is in question
        AnsIdx++;
      }
      if (AnsIdx>0) {
        NGramString=docText.substring(beginpos,endpos);
        totalNum[AnsIdx-1]++;
        //totalNum[AnsIdx-1]+=NGAnnotation.getElements().size();
        //check if the NGram is found in question
        found=false;
        for (String s : QueNGramStr) {
          if (s.equals(NGramString)) {found=true;break;}
        }
        if (found) {overlapNum[AnsIdx-1]++;}      
        //if (found) {overlapNum[AnsIdx-1]+=NGAnnotation.getElements().size();}
      }
    }
    
    //add the stanford NLP
    
    
    JFSIndexRepository repo = aJCas.getJFSIndexRepository();
    FSIterator<TOP> NEiter = repo.getAllIndexedFS(NamedEntityMention.type);
  
    ArrayList<String> QentityS=new ArrayList<String>(); 
    AnsIdx=0;
    String NEString;
    
    while(NEiter.hasNext()){
      NamedEntityMention NE=(NamedEntityMention) NEiter.next();
      beginpos=NE.getBegin();
      endpos=NE.getEnd();
      NEString=NE.getCoveredText();

      if (beginpos>QAEnd[AnsIdx]){ //if the end of the NGram is before the start of the first answer, then the NGram is in question
        AnsIdx++;
      }
      
      if (AnsIdx==0) {
        if (NEString!=null) {QentityS.add(NEString);}       
      }
      if (AnsIdx>0 && beginpos>=QABegin[AnsIdx]) {
        if (NEString!=null) {
          totalNum[AnsIdx-1]++;

          //check if the NGram is found in question
          found=false;
         
          for (String s : QentityS) {
            if (s.equals(NEString)) {found=true;break;}
          }
          if (found) {overlapNum[AnsIdx-1]++;}
        }

      }
      
    } 
    
    ////////////////////////////////////////////////////////////////////////////
    
    
    
    
    
    //calculate scores = overlapNum/totalNum, create AnswerScoreAnnotator
    FSIterator<org.apache.uima.jcas.tcas.Annotation> AnsIterator2 = aJCas.getAnnotationIndex(Answer.type).iterator();
    count=0;

    while(AnsIterator2.hasNext()){
      //find the question annotation
      Answer AnsAnnotation=(Answer) AnsIterator2.next(); 
      
      AnswerScore annotation = new AnswerScore(aJCas);
      annotation.setBegin(AnsAnnotation.getBegin());
      annotation.setEnd(AnsAnnotation.getEnd());
      annotation.setConfidence(1.0);
      annotation.setCasProcessorId("AnswerScoreAnnotator");
      annotation.setAnswer(AnsAnnotation);
      double score=(double)overlapNum[count]/(double)totalNum[count];
      annotation.setScore(score);
      //add the annotation to index
      annotation.addToIndexes();    
      count++;
    }   
    
  }

}


