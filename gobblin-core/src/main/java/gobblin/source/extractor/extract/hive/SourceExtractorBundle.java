package gobblin.source.extractor.extract.hive;

import java.io.IOException;
import java.util.List;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import gobblin.configuration.SourceState;
import gobblin.configuration.WorkUnitState;
import gobblin.source.Source;
import gobblin.source.extractor.Extractor;
import gobblin.source.workunit.WorkUnit;


/**
 * A DataSource, Extractor bundle that functions like a {@link Source}.
 * This allows the DataSource and Extractors to be combined in different ways.
 */
public class SourceExtractorBundle<S,D> implements Source<S,D> {

  private DataSource dataSource = null;
  private Extractor<S,D> extractor = null;

  public SourceExtractorBundle(DataSource dataSource, Extractor<S,D> extractor)
  {
    this.dataSource = dataSource;
    this.extractor = extractor;
  }

  /**
   * Get a list of {@link WorkUnit}s, each of which is for extracting a portion of the data.
   *
   * <p>
   *   Each {@link WorkUnit} will be used instantiate a {@link WorkUnitState} that gets passed to the
   *   {@link #getExtractor(WorkUnitState)} method to get an {@link Extractor} for extracting schema
   *   and data records from the source. The {@link WorkUnit} instance should have all the properties
   *   needed for the {@link Extractor} to work.
   * </p>
   *
   * <p>
   *   Typically the list of {@link WorkUnit}s for the current run is determined by taking into account
   *   the list of {@link WorkUnit}s from the previous run so data gets extracted incrementally. The
   *   method {@link SourceState#getPreviousWorkUnitStates} can be used to get the list of {@link WorkUnit}s
   *   from the previous run.
   * </p>
   *
   * @param state see {@link SourceState}
   * @return a list of {@link WorkUnit}s
   */
  @Override
  public List<WorkUnit> getWorkunits(SourceState state) {
    Config config = ConfigFactory.parseProperties(state.getProperties());
    initializeDataSource(config);
    initializeExtractor(config); // This is not strictly needed, but good to catch extractor init issues early

    return dataSource.getWorkunits(state);
  }

  private void initializeDataSource(Config config) {
    //TODO: What happens if datasource is already initialized?

    if (config.hasPath(HiveConfigurationKeys.CONFIG_KEY_DATASOURCE_CLASS))
    {
      String dataSourceClassName = config.getString(HiveConfigurationKeys.CONFIG_KEY_DATASOURCE_CLASS);
      try
      {
        dataSource = (DataSource) Class.forName(dataSourceClassName).newInstance();
      }
      catch (ClassNotFoundException cnfe)
      {
        throw new RuntimeException("Could not find class " + dataSourceClassName + " ", cnfe);
      } catch (InstantiationException e) {
        throw new RuntimeException("Could not instantiate class " + dataSourceClassName + " ", e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Could not access class " + dataSourceClassName + " ", e);
        }
    }
    else
    {
      throw new RuntimeException(new InvalidConfigException(
          String.format("Need a datasource class specified as %s. Found none.",
              HiveConfigurationKeys.CONFIG_KEY_DATASOURCE_CLASS)));
    }

    try {
      dataSource.initialize(config);
    } catch (InvalidConfigException ice) {
      throw new RuntimeException("Could not initialize the data source", ice);
    }
  }


  private void initializeExtractor(Config config) {
    if (config.hasPath(HiveConfigurationKeys.CONFIG_KEY_EXTRACTOR_CLASS))
    {
      String extractorClassName = config.getString(HiveConfigurationKeys.CONFIG_KEY_EXTRACTOR_CLASS);
      try
      {
         extractor = (Extractor<S, D>) Class.forName(extractorClassName).newInstance();
      }
      catch (ClassNotFoundException cnfe)
      {
        throw new RuntimeException("Could not find class " + extractorClassName + " ", cnfe);
      } catch (InstantiationException e) {
        throw new RuntimeException("Could not instantiate class " + extractorClassName + " ", e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Could not access class " + extractorClassName + " ", e);
      }
    }
    else
    {
      throw new RuntimeException(new InvalidConfigException(
          String.format("Need a extractor class specified as %s. Found none.",
              HiveConfigurationKeys.CONFIG_KEY_DATASOURCE_CLASS)));
    }
  }


  /**
   * Get an {@link Extractor} based on a given {@link WorkUnitState}.
   *
   * <p>
   *   The {@link Extractor} returned can use {@link WorkUnitState} to store arbitrary key-value pairs
   *   that will be persisted to the state store and loaded in the next scheduled job run.
   * </p>
   *
   * @param state a {@link WorkUnitState} carrying properties needed by the returned {@link Extractor}
   * @return an {@link Extractor} used to extract schema and data records from the data source
   * @throws IOException if it fails to create an {@link Extractor}
   */
  @Override
  public Extractor<S, D> getExtractor(WorkUnitState state)
      throws IOException {
    //TODO: is this the right behavior? What if getExtractor gets called with different state?
    if (extractor == null)
    {
      Config config = ConfigFactory.parseProperties(state.getProperties());
      initializeExtractor(config);
    }
    return this.extractor;
  }

  /**
   * Shutdown this {@link Source} instance.
   *
   * <p>
   *   This method is called once when the job completes. Properties (key-value pairs) added to the input
   *   {@link SourceState} instance will be persisted and available to the next scheduled job run through
   *   the method {@link #getWorkunits(SourceState)}.  If there is no cleanup or reporting required for a
   *   particular implementation of this interface, then it is acceptable to have a default implementation
   *   of this method.
   * </p>
   *

   * @param state see {@link SourceState}
   */
  @Override
  public void shutdown(SourceState state) {
    this.dataSource.shutdown(state);
  }
}
