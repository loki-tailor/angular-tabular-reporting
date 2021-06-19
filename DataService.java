import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class DataService {

    public static void main(String[] args) throws IOException {

        // paths
        String csv_path = "data.csv";

        String dist_path = "angular-tabular-reporting/dist/angular-tabular-reporting";
        String out_dir = "out/";

        String mainjs_name = "main.b7b0f4466cf28d987c2f.js";
        String mainjs_path = dist_path + "/{template}" + mainjs_name;

        String stylecss_fname = "styles.0e91bafdcc9ddd072e07.css";
        String stylecss_path = dist_path + "/" + stylecss_fname;
        String stylecss_data = FileOps.readFile(stylecss_path);

        // read csv data
        String csv_data = FileOps.readFile(csv_path);

        // exit if could not read data
        if (Objects.isNull(csv_data)) {
            System.out.println("csv_data == null, bye...");
            System.exit(-1);
        }

        // convert csv data to singile line tsv
        // i.e replace \n with |
        // this is helpful in injecting the tsv in .js
        // this tsv will be converted to json in angular script
        String csv_data_as_tsv_for_json = csv_data.trim()//
                .replace("\r", "") //
                .replace("\n", "|")//
                .replace("'", "\\'");

        // assuming the 1 entry in csv is a column header
        // get header rows
        String[] csv_headers = csv_data.split("\n")[0].split(",");

        String headers_as_json = get_headers_as_json_array_for_ag_grid_reporting(csv_headers);

        // read template mainjs
        String mainjs_data = FileOps.readFile(mainjs_path);

        // terminate if no template exists
        if (Objects.isNull(mainjs_data)) {
            System.out.println("mainjs.data is null @ " + mainjs_path);
            System.exit(-1);
        }

        // replace templte data with the required one
        String mainjs_updated_data = mainjs_data//
                .replace("{csv.data}", csv_data_as_tsv_for_json)//
                .replace("{column.headers}", headers_as_json);

        // create destination dir with temp uuid
        UUID uid = UUID.randomUUID();
        Path out = Paths.get(out_dir + "reports");
        FileOps.mkdir(out, true);

        // copy supporting files fromm original-build
        FileOps.copyDir(Paths.get(dist_path), out, StandardCopyOption.REPLACE_EXISTING);

        // write the updated mainjs to new path
        FileOps.write(out + "/" + mainjs_name, mainjs_updated_data);

        // highlight columns with 'FAIL: '
        Set<Integer> set = new HashSet<>();
        set.add(1);
        set.add(5);
        // ideally above indexes will come by iterating over each row
        // and then check if it contains "FAIL:" keyword
        // if yes then that row is a failure

        if (set.isEmpty() == false) {
            String tmp_1 = "{cols} { background-color: #690000; }";
            String tmp_2 = ".ag-header-cell[col-id=\"{col_name}\"]";
            List<String> tmp_cols = new ArrayList<>();
            for (Integer i : set) {
                tmp_cols.add(tmp_2.replace("{col_name}", csv_headers[i]));
            }

            String ag_header_cols = tmp_cols.stream().reduce((a, b) -> a + "," + b).get();
            String style_css_updated_data = stylecss_data + tmp_1.replace("{cols}", ag_header_cols);
            FileOps.write(out + "/" + stylecss_fname, style_css_updated_data);
        }

        // print index.html path
        String index_html = out.toAbsolutePath() + "/index.html";
        System.out.println("reports created @ " + index_html);

        System.out.println("finished execution");
    }

    // ============================================================================================================
    private static String skip_cols_path = "columns_to_hide_on_1st_load.txt";
    private static List<String> col_to_hide = new ArrayList<>();

    private static String get_headers_as_json_array_for_ag_grid_reporting(String[] headers_arr) {

        if (col_to_hide.isEmpty()) {
            String[] d = FileOps.readFile(skip_cols_path).trim().split("\n");
            for (String a : d)
                col_to_hide.add(a.trim());
        }
        System.out.println("columsn to hide = " + col_to_hide);

        JsonArray ja = new JsonArray();

        for (String col : headers_arr) {

            col = col.trim();

            JsonObject jo = new JsonObject();
            jo.addProperty("headerName", col);
            jo.addProperty("field", col);
            if (col_to_hide.contains(col))
                jo.addProperty("hide", "!0,");
            ja.add(jo);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(ja);

        return json.replace("\"!0,\"", "!0,") //
                .replace("\"headerName\"", "headerName") //
                .replace("\"field\"", "field");
    }

}

class FileOps {

    // ------------------------------------------------------------------------------------------
    /***
     * 
     * @param source
     * @param target
     * @param options
     * @throws IOException
     * 
     * @ref: http://tutorials.jenkov.com/java-nio/files.html#files-walkfiletree
     * @ref: https://stackoverflow.com/a/60621544/7415499
     * @ref :
     *      https://www.geeksforgeeks.org/path-resolve-method-in-java-with-examples/
     * @ref :
     *      https://www.geeksforgeeks.org/path-relativize-method-in-java-with-examples/
     */
    public static void copyDir(Path source, Path target, CopyOption... options) throws IOException {

        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                // new dir path where data is to be copied
                // could be present or absent
                Path resolve = target.resolve(source.relativize(dir));
                File resolve_file = resolve.toFile();

                if (resolve_file.exists() == false) {
                    Files.createDirectory(resolve);
                } else {
                    // delete old data in sub-directories
                    // without deleting the directory
                    Arrays.asList(resolve_file.listFiles()).forEach(File::delete);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // ------------------------------------------------------------------------------------------

    /*
     * Written by Satish Kumar on Friday 1/7/2016
     * 
     */

    // Use this method to create new file or overwrite all the existing content to
    // write new content passed to the method
    public static void write(String path, String content) {
        try {
            File file = new File(path);
            // If file doesn't exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            // Write in file
            bw.write(content);
            // Close connection
            bw.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // ------------------------------------------------------------------------------------------

    // Call this method to append data to the existing file
    public static synchronized void append(String path, String content) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(path, true)));
            out.println(content);
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    public static void mkdir(final Path dirToCreate, boolean purge_old_data) throws IOException {

        File dir_file = dirToCreate.toFile();

        if (dir_file.exists())

            if (purge_old_data)

                // for each-sub directory
                // delete its content
                Files.walkFileTree(dirToCreate, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        System.out.println("delete file: " + file.toString());
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

                        Files.delete(dir);
                        System.out.println("delete dir: " + dir.toString());
                        return FileVisitResult.CONTINUE;
                    }
                });
            else {
                System.out.println(
                        "dir exists with some data, caller requested not to delete @ " + dirToCreate.toAbsolutePath());
                return;
            }

        Files.createDirectory(dirToCreate);
        System.out.println("created dir: " + dirToCreate.toAbsolutePath());

    }

    // ------------------------------------------------------------------------------------------

    public static void mkdir(final Path path) {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) == false) {
            try {
                Files.createDirectory(path.toAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            System.out.println("directory created @ " + path.toAbsolutePath());
        } else {
            System.out.println("directory already exists, not recreating @ " + path.toAbsolutePath());
        }
    }

    // ------------------------------------------------------------------------------------------

    private static void createDir(final String dirName) {
        try {
            final File homeDir = new File(System.getProperty("user.home"));
            final File dir = new File(homeDir, dirName);
            if (!dir.exists() && !dir.mkdirs()) {
                System.out.println("Couldn't create dir:" + dir.getAbsolutePath());
            }
            System.out.println("dir created @ " + dir.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------------------------------

    public static String readFile(final String filePath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            br.close();
            return sb.toString();

        } catch (Exception e) {

        }
        return null;
    }

    // ------------------------------------------------------------------------------------------

    public static List<String> readFileAsList(final String filePath) {

        List<String> list = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                list.add(line);
                line = br.readLine();
            }
            br.close();
            return list;

        } catch (Exception e) {

        }
        return list;
    }

    // ------------------------------------------------------------------------------------------

    public static void deleteFile(final String filePath) {
        try {

            final File file = new File(filePath);
            if (file.exists() && file.delete()) {
                System.out.println("Deleted File:" + file.getAbsolutePath());
            } else {
                System.out.println("Couldn't delete file:" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("hello world");
        Path d = Paths.get("src/res/fv/copy");
        try {
            mkdir(d, true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("done");
    }
}
