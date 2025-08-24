/*
* Copyright 2024 - 2024 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package se.telavox.mcp;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

	private static final String BASE_URL = "https://api.weather.gov";

	private final OkHttpClient client;
	private final Moshi moshi;

	public WeatherService() {

		this.client = new OkHttpClient();
		this.moshi = new Moshi.Builder().build();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Points(@JsonProperty("properties") Props properties) {
		@JsonIgnoreProperties(ignoreUnknown = true)
		public record Props(@JsonProperty("forecast") String forecast) {
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Forecast(@JsonProperty("properties") Props properties) {
		@JsonIgnoreProperties(ignoreUnknown = true)
		public record Props(@JsonProperty("periods") List<Period> periods) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record Period(@JsonProperty("number") Integer number, @JsonProperty("name") String name,
				@JsonProperty("startTime") String startTime, @JsonProperty("endTime") String endTime,
				@JsonProperty("isDaytime") Boolean isDayTime, @JsonProperty("temperature") Integer temperature,
				@JsonProperty("temperatureUnit") String temperatureUnit,
				@JsonProperty("temperatureTrend") String temperatureTrend,
				@JsonProperty("probabilityOfPrecipitation") Map probabilityOfPrecipitation,
				@JsonProperty("windSpeed") String windSpeed, @JsonProperty("windDirection") String windDirection,
				@JsonProperty("icon") String icon, @JsonProperty("shortForecast") String shortForecast,
				@JsonProperty("detailedForecast") String detailedForecast) {
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Alert(@JsonProperty("features") List<Feature> features) {

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record Feature(@JsonProperty("properties") Properties properties) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record Properties(@JsonProperty("event") String event, @JsonProperty("areaDesc") String areaDesc,
				@JsonProperty("severity") String severity, @JsonProperty("description") String description,
				@JsonProperty("instruction") String instruction) {
		}
	}

	/**
	 * Get forecast for a specific latitude/longitude
	 * 
	 * @param latitude  Latitude
	 * @param longitude Longitude
	 * @return The forecast for the given location
	 * @throws RestClientException if the request fails
	 */
	@Tool(description = "Get weather forecast for a specific latitude/longitude")
	public String getWeatherForecastByLocation(double latitude, double longitude) {

		final JsonAdapter<Points> pointsJsonAdapter = moshi.adapter(Points.class);
		var request = new Request.Builder()
				.url(BASE_URL + "/points/" + latitude + "," + longitude)
				.addHeader("Accept", "application/geo+json")
				.addHeader("User-Agent", "WeatherApiClient/1.0 (jesper.hasselquist@telavox.com)")
				.build();

		Points points;
		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			points = pointsJsonAdapter.fromJson(response.body().source());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		final JsonAdapter<Forecast> forecastJsonAdapter = moshi.adapter(Forecast.class);
		var request2 = new Request.Builder()
				.url(BASE_URL + points.properties().forecast())
				.addHeader("Accept", "application/geo+json")
				.addHeader("User-Agent", "WeatherApiClient/1.0 (jesper.hasselquist@telavox.com)")
				.build();

		String forecastText;
		try (Response response = client.newCall(request2).execute()) {
			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			var forecast = forecastJsonAdapter.fromJson(response.body().source());
			forecastText = forecast.properties().periods().stream().map(p -> {
				return String.format("""
						%s:
						Temperature: %s %s
						Wind: %s %s
						Forecast: %s
						""", p.name(), p.temperature(), p.temperatureUnit(), p.windSpeed(), p.windDirection(),
						p.detailedForecast());
			}).collect(Collectors.joining());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return forecastText;
	}

	/**
	 * Get alerts for a specific area
	 * 
	 * @param state Area code. Two-letter US state code (e.g. CA, NY)
	 * @return Human readable alert information
	 * @throws RestClientException if the request fails
	 */
	@Tool(description = "Get weather alerts for a US state. Input is Two-letter US state code (e.g. CA, NY)")
	public String getAlerts(String state) {

		final JsonAdapter<Alert> alertJsonAdapter = moshi.adapter(Alert.class);
		var request = new Request.Builder()
				.url(BASE_URL + "/alerts/active/area/" + state)
				.addHeader("Accept", "application/geo+json")
				.addHeader("User-Agent", "WeatherApiClient/1.0 (jesper.hasselquist@telavox.com)")
				.build();

		Alert alert;
		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			alert = alertJsonAdapter.fromJson(response.body().source());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return alert.features()
				.stream()
				.map(f -> String.format("""
						Event: %s
						Area: %s
						Severity: %s
						Description: %s
						Instructions: %s
						""", f.properties().event(), f.properties.areaDesc(), f.properties.severity(),
						f.properties.description(), f.properties.instruction()))
				.collect(Collectors.joining("\n"));
	}

	public static void main(String[] args) {
		WeatherService client = new WeatherService();
		System.out.println(client.getWeatherForecastByLocation(47.6062, -122.3321));
		System.out.println(client.getAlerts("NY"));
	}

}
