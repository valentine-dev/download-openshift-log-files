import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 
 * @author Valentine Wu
 *
 */
public class OpenShiftLogFilesDownloader {

	final static private String NEW_LINE = System.lineSeparator();
	final static private String[] COMMANDS = { 
			"oc get pods --no-headers=true -o name", 
			"oc exec POD_NAME -- ls",
			"oc cp POD_NAME:LOG_FILE POD_NAME-LOG_FILE" };
	final static private String POD_NAME = "POD_NAME";
	final static private String LOG_FILE = "LOG_FILE";
	static private String DATE_STRING = null;
	static private Map<String, List<String>> filesInPod = new HashMap<>();

	public static void main(String[] args) {

		// input validation
		ensureCorrectUsage(args);

		// business logic
		for (String command : COMMANDS) {
			log(NEW_LINE + "[INPUT] START >>>>> - [" + command + "]");
			process(command);
			log("[INPUT] END   >>>>> - [" + command + "]" + NEW_LINE);
		}
	}

	private static void process(String inputCommand) {

		// convert command
		List<String> commandList = convertCommand(inputCommand);
		
		// execute commands
		executeCommandList(commandList);
	}

	private static void executeCommandList(List<String> commandList) {
		for (String command : commandList) {
			log(NEW_LINE + "[EXECUTION] START ----> [" + command + "]");
			try {
				Process process = new ProcessBuilder("cmd.exe", "/c", command).start();
				process.waitFor();
				BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
	
				String line;
				boolean error = false;
				while ((line = stdError.readLine()) != null) {
					log("[EXECUTION] - Error output: [" + line + "]");
					error = true;
				}
				if (error) {
					error("[EXECUTION]There is error!");
					System.exit(0);
				}
				
				while ((line = stdInput.readLine()) != null) {
					processOutput(command, line);
				}
	
			} catch (Exception e) {
				error("[EXECUTION] - Exception: " + e.getMessage());
			}
			log("[EXECUTION] END   ----> [" + command + "]" + NEW_LINE);
		}
	}

	private static void processOutput(String command, String output) {
		log(NEW_LINE + "[OUTPUT] START <<<<< [" + output + "]");
		if (command.startsWith("oc get pods")) {
			String podName = output.substring(output.indexOf('/')+1);
			log("[OUTPUT] - Pod name: [" + podName + "]");
			filesInPod.put(podName, new ArrayList<>());
		} else if (command.startsWith("oc exec ")) {
			String key = command.substring("oc exec ".length(), command.indexOf(" --"));
			if (DATE_STRING == null || output.contains(DATE_STRING)) {
				if (filesInPod.get(key) != null) {
					filesInPod.get(key).add(output);
					log("[OUTPUT] - Log file: [" + output + "]");
				} else {
					error("[OUTPUT] - Error when adding log file");
					System.exit(0);
				}
			}
		} else if (command.startsWith("oc cp ")) {
			log("[OUTPUT] - File copied!");
		} else {
			error("[OUTPUT] - Error when processing output");
			System.exit(0);
		}
		log("[OUTPUT] END   <<<<< [" + output + "]" + NEW_LINE);
	}

	private static List<String> convertCommand(String command) {
		log(NEW_LINE + "[CONVERSION] START ***** [" + command + "]");
		List<String> commands = new ArrayList<>();
		if (filesInPod.isEmpty()) {
			commands.add(command);
			log("[CONVERSION]Command [" + command + "] added into execution list.");
		} else {
			filesInPod.forEach((key, value) -> {
				log("[CONVERSION]Pod name is [" + key + "]");
				if(command.contains(POD_NAME)) {
					String convertedCommand = command.replace(POD_NAME, key);
					log("[CONVERSION]Command converted to [" + convertedCommand + "]");
					if(!value.isEmpty()) {
						for (String file : value) {
							if (convertedCommand.contains(LOG_FILE)) {
								String convertedTwiceCommand = convertedCommand.replace(LOG_FILE, file);
								log("[CONVERSION]Command converted to [" + convertedTwiceCommand + "]");
								commands.add(convertedTwiceCommand);
								log("[CONVERSION]Command [" + convertedTwiceCommand + "] added into execution list.");
							} else {
								commands.add(convertedCommand);
								log("[CONVERSION]Command [" + convertedCommand + "] added into execution list.");
							}
						}
					} else {
						commands.add(convertedCommand);
						log("[CONVERSION]Command [" + convertedCommand + "] added into execution list.");
					}
				} else {
					commands.add(command);
					log("[CONVERSION]Command [" + command + "] added into execution list.");
				}
			});
		}
		log("[CONVERSION] END   ***** [" + command + "]" + NEW_LINE);
		return commands;
	}

	private static void log(String message) {
		System.out.println(message);
	}
	
	private static void error(String message) {
		System.out.println(message);
	}

	private static void ensureCorrectUsage(String[] args) {
		log(NEW_LINE + "Usage 1: OpenShiftLogFilesDownloader [yyyy-mm-dd] - download log files on specific day after logging in");
		log("Usage 2: OpenShiftLogFilesDownloader - download all log files after logging in");
		if (args.length > 2) {
			error("Wrong Usage!");
			System.exit(0);
		}
		
		if (args.length == 1) {
			DATE_STRING = args[0];
		}
		
		promptEnterKey();
	}
	
	private static void promptEnterKey(){
	   log(NEW_LINE + "Press \"ENTER\" to continue... or CTRL+C to exit");
	   @SuppressWarnings("resource")
	   Scanner scanner = new Scanner(System.in);
	   scanner.nextLine();
	}
}
