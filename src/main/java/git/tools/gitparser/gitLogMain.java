package git.tools.gitparser;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

public class gitLogMain {
String log4jConfPath = "log4j.properties";

private static List<Thread> lstOfThreads;
    private static boolean error = true;
    
    public static void main(String[] args) {
        lstOfThreads = new ArrayList<>();        

        if(args.length == 2){
            if(args[0].equalsIgnoreCase("-e"))
                error = false;
            else
                System.out.println("Параметры не верны");
            if(Files.exists(Paths.get(args[1]))){
                threadGo(args[1]);
            }          
            else
                System.out.println("Папка не существует");
        } else if(args.length == 1){
            if(args[0].equalsIgnoreCase("-e"))
                error = false;
            else if(Files.exists(Paths.get(args[0])))
                threadGo(args[0]);
            else
                System.out.println("Параметер некорректен");
        } 
        showCmd();
        
    }
    
    private static ArrayList<String> loadStatus(){
        ArrayList<String> namesList = new ArrayList<String>();
         
        try
        {
            try (FileInputStream fis = new FileInputStream("listData"); 
                    ObjectInputStream ois = new ObjectInputStream(fis)) {
                
                namesList = (ArrayList) ois.readObject();
                
            }
        }
        catch (IOException ioe)
        {
            System.out.println("Не могу загрузить прогресс парсинга! " + ioe.getMessage());
            return namesList;
        }
        catch (ClassNotFoundException c)
        {
            System.out.println("Класс не найден " + c.getMessage());          
            return namesList;
        }
        return namesList;
    }
    
    private static void saveStatus(ArrayList<String> namesList){        
             try (FileOutputStream fos = new FileOutputStream("listData");
                     ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                 oos.writeObject(namesList);
             }        
        catch (IOException ioe)
        {
            System.out.println("Не могу сохранить прогресс парсинга! " + ioe.getMessage());
        }
    }
    
    private static void threadGo(String gitpath){
        Thread parseThread = new Thread(new parseThread(gitpath,loadStatus(),error));
        lstOfThreads.add(parseThread);
        parseThread.start();
    }
    
    private static void threadsStop(){
        lstOfThreads.stream().forEach(Thread::interrupt);
    }
    
    private static void showCmd(){
        String ans;
            Scanner in = new Scanner(System.in);
            do{
                System.out.print("- ");            
                ans = in.nextLine();
                if(ans.equalsIgnoreCase("?")){
                    System.out.println("add {gitpath} - добавить папку для парсинга");
                    System.out.println("stop - остановить парсинг и выйти");
                }
                if(ans.toLowerCase().startsWith("add")){
                    String tmp[] = ans.split(" ");
                    for(int i = 1; i < tmp.length; ++i)
                        threadGo(tmp[i]);
                }                                   
            }while(!ans.equalsIgnoreCase("stop"));
            threadsStop();
    }
   
 
}
