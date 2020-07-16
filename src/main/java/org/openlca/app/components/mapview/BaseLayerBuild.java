package org.openlca.app.components.mapview;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import org.openlca.geo.geojson.GeoJSON;
import org.openlca.geo.geojson.ProtoPack;

/**
 * Generates our base layers from raw data.
 */
public class BaseLayerBuild {

	public static void main(String[] args) {

		// define the download paths and target files
		var base = "https://raw.githubusercontent.com/martynafford/natural-earth-geojson/master/50m";
		var paths = List.of(
				"/cultural/ne_50m_admin_0_countries.json",
				"/physical/ne_50m_lakes.json",
				"/physical/ne_50m_land.json",
				"/physical/ne_50m_ocean.json"
		);
		var targets = List.of(
				"countries",
				"lakes",
				"land",
				"oceans"
		);

		// create the HTTP client
		var http = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_2)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(40))
				.build();

		// download files and compress them
		for (int i = 0; i < paths.size(); i++) {
			var geojson = Paths.get("raw_data", targets.get(i) + ".geojson");
			var url = base + paths.get(i);
			fetch(url, geojson, http);

			var protopack = Paths.get(
					"./src/main/resources/org/openlca/app/components/mapview/"
							+ targets.get(i) + ".protopack.gz");
			var features = GeoJSON.read(geojson.toFile());
			var packed = ProtoPack.packgz(features);
			System.out.println("Write protopack " + protopack);
			try {
				Files.write(protopack, packed);
			} catch (Exception e) {
				throw new RuntimeException(
						"failed to write " + protopack, e);
			}
		}
	}

	private static void fetch(String url, Path file, HttpClient client) {
		if (Files.exists(file)) {
			System.out.println("File " + file + " exists");
			return;
		}
		try {
			System.out.println("Download " + file + " from " + url);
			var req = HttpRequest.newBuilder(new URI(url)).GET().build();
			var resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
			try {
				Files.write(file, resp.body());
			} catch (Exception io) {
				throw new RuntimeException("failed to write to file " + file, io);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
