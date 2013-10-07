package edu.cmu.deiis.CPE;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.resource.ResourceProcessException;
/*
public class evaluatorCasConsumer extends CasConsumer_ImplBase {

  @Override
  public void processCas(CAS arg0) throws ResourceProcessException {
    // TODO Auto-generated method stub

  }

}
*/





import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;

import edu.cmu.deiis.annotator.evaluator.ASComparator;
import edu.cmu.deiis.types.Answer;
import edu.cmu.deiis.types.AnswerScore;
import edu.cmu.deiis.types.Question;

/**
 * A simple CAS consumer that writes the CAS to XMI format.
 * <p>
 * This CAS Consumer takes one parameter:
 * <ul>
 * <li><code>OutputDirectory</code> - path to directory into which output files will be written</li>
 * </ul>
 */
public class evaluatorCasConsumer extends CasConsumer_ImplBase {
  /**
   * Name of configuration parameter that must be set to the path of a directory into which the
   * output files will be written.
   */
  public static final String PARAM_OUTPUTDIR = "OutputDirectory";

  private File mOutputDir;

  private int mDocNum;
  
  /*
  public void initialize() throws ResourceInitializationException {
    mDocNum = 0;
    mOutputDir = new File((String) getConfigParameterValue(PARAM_OUTPUTDIR));
    if (!mOutputDir.exists()) {
      mOutputDir.mkdirs();
    }
  }
  */
  /**
   * Processes the CAS which was populated by the TextAnalysisEngines. <br>
   * In this case, the CAS is converted to XMI and written into the output file .
   * 
   * @param aCAS
   *          a CAS which has been populated by the TAEs
   * 
   * @throws ResourceProcessException
   *           if there is an error in processing the Resource
   * 
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(org.apache.uima.cas.CAS)
   */
  public void processCas(CAS aCAS) throws ResourceProcessException {
    String modelFileName = null;

    JCas jcas;
    try {
      jcas = aCAS.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    // retrieve the filename of the input file from the CAS
    String docText=jcas.getDocumentText();
    FSIterator<org.apache.uima.jcas.tcas.Annotation> QueIterator = jcas.getAnnotationIndex(Question.type).iterator();
    FSIterator<org.apache.uima.jcas.tcas.Annotation> ASIterator = jcas.getAnnotationIndex(AnswerScore.type).iterator();
    
    //print the question
    int QAnum=1+jcas.getAnnotationIndex(Answer.type).size();
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

    // serialize XCAS and write to output file

  }
  
  public class ASComparator implements Comparator<AnswerScore> {
    public int compare(AnswerScore r1, AnswerScore r2) {
      //return (int)(r1.start-r2.start); //increasing

      double diff=-r1.getScore()+r2.getScore();
      if (diff>0) {return 1;}
      if (diff==0) {return 0;}
      return -1;

    }
  }

}
