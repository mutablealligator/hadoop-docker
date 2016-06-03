import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.log4j.Logger;

public class BigramCount {
	private static final Logger LOG = Logger.getLogger(BigramCount.class);

	protected static class MyMapper extends Mapper<LongWritable, Text, Text, IntWritable>  {
		private static final IntWritable one = new IntWritable(1);
		private static final Text bg = new Text();

		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();

			String previous = null;
			StringTokenizer itr = new StringTokenizer(line);
			while (itr.hasMoreTokens()) {
				String cur = itr.nextToken();
				if (previous != null) {
					bg.set(previous + "|" + cur);
					context.write(bg, one);
				}
				previous = cur;
			}
		}
	}

	protected static class MyReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
		private final static IntWritable SUM = new IntWritable();

		public void reduce(Text key, Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {
			int sum = 0;
			Iterator<IntWritable> iter = values.iterator();
			while (iter.hasNext()) {
				sum += iter.next().get();
			}
			SUM.set(sum);
			context.write(key, SUM);
		}
	}

	private BigramCount() {
	}

	private static int printUsage(String[] args) {
		System.out.println("BigramCount : inputPath outputPath #reducers");
		for (int i = 0; i < args.length; i++) {
			String param = args[i];
			System.out.println(param);
		}
		//ToolRunner.printGenericCommandUsage(System.out);
		return -1;
	}

	public static void run(String[] args, Configuration conf) throws Exception {
		if (args.length != 3) {
			printUsage(args);
			return;
		}

		String inputPath = args[0];
		String outputPath = args[1];
		int reduceTasks = Integer.parseInt(args[2]);

		Job job = Job.getInstance(conf);
		job.setJobName(BigramCount.class.getSimpleName());
		job.setJarByClass(BigramCount.class);

		job.setNumReduceTasks(reduceTasks);

		FileInputFormat.setInputPaths(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		job.setMapperClass(MyMapper.class);
		job.setCombinerClass(MyReducer.class);
		job.setReducerClass(MyReducer.class);

		Path outputDir = new Path(outputPath);
		FileSystem.get(conf).delete(outputDir, true);

		long startTime = System.currentTimeMillis();
		boolean ret = job.waitForCompletion(true);
		if(ret) {
			System.out.println("Job Finished in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
		} else {
			System.err.println("Job Failed to Complete!");
		}

	}

	static ArrayList<Entry<String, Integer>> entriesSortedByValues(HashMap<String, Integer> map) {

		ArrayList<Entry<String, Integer>> sortedEntries = new ArrayList<Entry<String, Integer>>(map.entrySet());

		Collections.sort(sortedEntries, new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> e1, Entry<String, Integer> e2) {
				Integer i1 = e1.getValue();
				Integer i2 = e2.getValue();
				
				if(i1 < i2)
					return 1;
				else if(i1 > i2)
					return -1;
				else
					return 0;
			}
		});

		return sortedEntries;
	}

	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();
		run(args, conf);
		
		HashMap<String, Integer> bigramCountMap = new HashMap<String, Integer>();
		Path outputPath = new Path(args[1]);
		
		FileSystem hdfs = FileSystem.get(conf);
		Path outpath = new Path("bigram-result.txt");
	        FileUtil.copyMerge(hdfs, outputPath, hdfs, outpath, false, conf, "");
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(FileSystem.get(conf).open(outpath)));
		String line = reader.readLine();
		int totalBigramOccurrences = 0;
		while (line != null) {
			System.out.println(line);
			String[] kvTokens = line.trim().split("[ \t\n\f\r]");
			String bigram = kvTokens[0];
			System.out.println(bigram);
			int count = Integer.parseInt(kvTokens[1]);
			totalBigramOccurrences += count;
			bigramCountMap.put(bigram, new Integer(count));
			line = reader.readLine();
		}
		
		
		ArrayList<Entry<String, Integer>> sortedEntries = entriesSortedByValues(bigramCountMap);
		
		System.out.println("Sorted Bigram Entries (Descending Popularity):-");
		for (Entry<String, Integer> entry : sortedEntries) {
			System.out.println("< " + entry.getKey() + " : " + entry.getValue().intValue() + " >");
		}
		System.out.println("\n\n");
		System.out.println("Total Bigram Occurrences: " + totalBigramOccurrences);
		System.out.println("Most Popular Bigram: " + sortedEntries.get(0).getKey());
		
		int totalTopN = 0;
		int topN = 0;
		for (Entry<String, Integer> entry : sortedEntries) {
			totalTopN += entry.getValue().intValue();
			topN++;
			
			if(totalTopN >= (int)(0.1 * (float)(totalBigramOccurrences))) {
				break;
			}
		}
		
		System.out.println("Number of Top-N Bigrams <= 10% of all bigram counts: " + topN);
	}
}
