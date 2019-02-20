package git.tools.gitparser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;


public class parseThread implements Runnable {
    final private String gitpath;
    private ArrayList<String> namesList;
    final private  boolean error;
    final int TIME_REFRESH = 10000;
    
    public parseThread(String gitpath, ArrayList<String>  namesList, boolean error){
        this.gitpath = gitpath;
        this.namesList = namesList;
        this.error = error;
    }
    
    public parseThread(String gitpath, ArrayList<String>  namesList){
        this.gitpath = gitpath;
        this.namesList = namesList;
        this.error = true;
    }
    
    public parseThread(String gitpath){
        this.gitpath = gitpath;
        this.namesList = new ArrayList<>();
        this.error = true;
    }
    
    @Override
    public void run() {
        int P = 0, F = 0, B = 0, BLD = 678;
        String template1 = "^!|^fix|^#|^refactor";
        String template2 = "^\\+|^add|^\\-|^remove|^\\*|^change";
        List<String> strToLog = new ArrayList<>();
            
        try (Repository existingRepo = new FileRepositoryBuilder()
                    .setGitDir(new File(gitpath))
                    .build();){
                
            try(Git git = new Git(existingRepo)){
                   
                while(true){
                    if(Thread.interrupted()) return;
                    //strToLog.addAll(getBranches(git));
                    List<Ref> branches = git.branchList().call();
                
                    for(Ref branch : branches ){
                        Iterable<RevCommit> logs = git.log().add(existingRepo.resolve(branch.getName())).call();
                    
                    //Iterable<RevCommit> logs = git.log().call();
                    
                    for (RevCommit rev : logs) {
                        if(namesList.contains(rev.getName())) continue;
                        
                        if(error & rev.getName().toLowerCase().contains("~/current/daily")&
                                (branch.getName().contains("master")||branch.getName().contains("develop")))
                            strToLog.add("Ошибка \"~/current/daily\" в ветке " + branch.getName());
                        
                        strToLog.add(String.valueOf(rev.getType()) + " full " + rev.getFullMessage() + " branch "+ existingRepo.getBranch());
                        if(checkWithRegExp(rev.getShortMessage(), template1))
                        {
                            B++;
                            strToLog.add(P+"."+F+"."+B+"."+BLD+" - " + rev.getFullMessage() + rev.getAuthorIdent().getName() +
                                    new Date(rev.getCommitTime() * 1000L).toString());
                            namesList.add(rev.getName());
                        }
                        else if(checkWithRegExp(rev.getShortMessage(), template2))
                        {
                            F++;
                            B = 0;
                            strToLog.add(P+"."+F+"."+B+"."+BLD+" - "+rev.getFullMessage() + rev.getAuthorIdent().getName() +
                                    new Date(rev.getCommitTime() * 1000L).toString());
                            namesList.add(rev.getName());
                        }
                    }
                    saveToFile(strToLog);
                    Thread.sleep(TIME_REFRESH);
                    }
                }
                } catch (GitAPIException ex) {
                    Logger.getLogger(gitLogMain.class.getName()).log(Level.SEVERE, null, ex);
                    saveStatus();
                } catch (InterruptedException ex) {
                    System.out.println("Парсинг завершен");
                    saveStatus();
                }
            } catch (IOException ex) {
                Logger.getLogger(gitLogMain.class.getName()).log(Level.SEVERE, null, ex);
                saveStatus();
            }
        saveStatus();
        }   
    
    public static ArrayList<String> getBranches(Git git) {
    try {
        final List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();

        final ArrayList<String> result = new ArrayList<>();
        refs.stream().forEach((ref) -> {
            result.add(ref.getName());
        });

        return result;
    } catch (GitAPIException e) {
        System.out.println("Ошибка при получении веток " + e.getLocalizedMessage());
        return new ArrayList<>();
    }
    }
    
    private static boolean checkWithRegExp(String userNameString, String template){  
        Pattern p = Pattern.compile(template);  
        Matcher m = p.matcher(userNameString);  
        return m.matches();  
    }
    
    private void saveStatus(){        
             try (FileOutputStream fos = new FileOutputStream("listData");
                     ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                 oos.writeObject(namesList);
             }        
        catch (IOException ioe)
        {
            System.out.println("Не могу сохранить прогресс парсинга! " + ioe.getMessage());
        }
    }
    
    private void saveToFile(List<String> strToLog) throws IOException{
        synchronized(this){
            if(!Files.exists(Paths.get("changelog.txt")))
                Files.createFile(Paths.get("changelog.txt"));
        try (BufferedWriter output = new BufferedWriter(new FileWriter("changelog.txt",true))) {
            for(String str : strToLog.toArray(new String[strToLog.size()]))
                output.append(str + System.getProperty("line.separator"));
            output.flush();
        }}
    }
}
