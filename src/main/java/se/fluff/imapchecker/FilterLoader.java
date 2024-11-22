package se.fluff.imapchecker;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FilterLoader {

    private LocalDateTime lastload = LocalDateTime.now();
    private int refreshinterval ;
    private String filename;
    private List<Junkfilter> filters = new ArrayList<>();

    public FilterLoader(String filename, int refreshinterval) {
        this.filename = filename;
        this.refreshinterval = refreshinterval;
        load();
    }

    /**
     * Forces a load of filters.
     * @return Count of how many filters are loaded
     */
    public int load() {

        InputStream is = null;
        File file = new File(filename);
        try {
            is = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            filters = new ArrayList<>();
            String line;
            while((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", 2);
                filters.add(new Junkfilter(parts[0], parts[1]));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        lastload = LocalDateTime.now();
        return filters.size();
    }

    /**
     * Tries to load the filters if at least refreshinterval seconds has passed
     * @return True if reloaded, false if not enough time has passed
     */
    public boolean tryLoad() {
        if(lastload.plusSeconds(refreshinterval).isAfter(LocalDateTime.now())) {
            return false;
        }
        System.out.println("Refreshing filters from file");
        load();
        System.out.println(filters.size() + " filters in memory");
        return true;
    }

    public List<Junkfilter> getFilters() {
        return filters;
    }

}
