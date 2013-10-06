package edu.cmu.deiis.annotator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;

import edu.cmu.deiis.types.Answer;
import edu.cmu.deiis.types.AnswerScore;
import edu.cmu.deiis.types.Question;

/**  
* evaluatorAnnotator.java - use the AnswerScore Annotations. 
* order the answer by score in descend order, calculate precision = (correct answer in first N answer)/(total correct answer number N) 
* @author  Ying Sheng
* @version 1.0 
*/ 

public class evaluator extends JCasAnnotator_ImplBase {

  public class ASComparator implements Comparator<AnswerScore> {
    public int compare(AnswerScore r1, AnswerScore r2) {
      //return (int)(r1.start-r2.start); //increasing
      return (int)(-r1.getScore()+r2.getScore());
    }
  }
  
  
  @Override
  
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
  //sort answer by score in descending, print to output file
    String docText=aJCas.getDocumentText();
    FSIterator<org.apache.uima.jcas.tcas.Annotation> QueIterator = aJCas.getAnnotationIndex(Question.type).iterator();
    FSIterator<org.apache.uima.jcas.tcas.Annotation> ASIterator = aJCas.getAnnotationIndex(AnswerScore.type).iterator();
    
    //print the question
    int QAnum=1+aJCas.getAnnotationIndex(Answer.type).size();
    int[] QABegin=new int[QAnum];
    int[] QAEnd=new int[QAnum]; 
    while(QueIterator.hasNext()){      
      Question QueAnnotation=(Question) QueIterator.next(); 
      QABegin[0]=QueAnnotation.getBegin();
      QAEnd[0]=QueAnnotation.getEnd();
    }
    String QuesStr;
    QuesStr=docText.substring(QABegin[0],QAEnd[0]);
    System.out.println("Question: "+QuesStr);
    
    //collect all the AnswerScore Annotation to the arraylist res;
    ArrayList<AnswerScore> res=new ArrayList<AnswerScore>(QAnum-1);
    while (ASIterator.hasNext()){
      AnswerScore ASAnnotation=(AnswerScore) ASIterator.next();
      res.add(ASAnnotation);
    }
    
    Collections.sort(res, new ASComparator());
     
    //calculate the N: # of correct answer
    int N=0;
    for (int i=0; i<QAnum-1; i++) {
      if (res.get(i).getAnswer().getIsCorrect()) {N++;}
    }
    
    //count the n: # of correct answer in the first N answer
    int n=0;
    for (int i=0; i<N; i++) {
      if (res.get(i).getAnswer().getIsCorrect()) {n++;}
    }
    
    //calculate precision by n/N
    double precision=(double)n/(double)N;
    
    //print sorted answer
    boolean golden;
    char isCorr;
    double score;
    String Ans;
    int start, end;
    for (int i=0; i<QAnum-1;i++) {
      golden=res.get(i).getAnswer().getIsCorrect();
      if (golden) {isCorr='+';} else {isCorr='-';}
      score=res.get(i).getScore();
      start=res.get(i).getAnswer().getBegin();
      end=res.get(i).getAnswer().getEnd();
      Ans=docText.substring(start,end);
      System.out.format("%c %.2f %s\n",isCorr,score,Ans);
    }
    
    System.out.format("Precision at %d: %.2f\n", N, precision);
    System.out.println();
    
  }

}
