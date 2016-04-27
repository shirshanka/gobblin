package gobblin.source.extractor.extract.hive;

import java.io.IOException;
import java.util.List;

import com.typesafe.config.Config;


import gobblin.configuration.SourceState;
import gobblin.configuration.WorkUnitState;
import gobblin.source.extractor.Extractor;
import gobblin.source.workunit.WorkUnit;


/**
 * An interface for a source that does not need to provide an extractor
 *
 */
public interface DataSource<T extends WorkUnit> {


  void initialize(Config config) throws InvalidConfigException;
  /**
   * Get a list of {@link WorkUnit}s, each of which is for extracting a portion of the data.
   *
   * <p>
   *   Each {@link WorkUnit} will be used instantiate a {@link gobblin.configuration.WorkUnitState} that gets passed to
   *   an {@link Extractor} for extracting data from the source. The {@link WorkUnit} instance should have all the properties
   *   needed for the {@link Extractor} to work.
   * </p>
   *
   * <p>
   *   Typically the list of {@link WorkUnit}s for the current run is determined by taking into account
   *   the list of {@link WorkUnit}s from the previous run so data gets extracted incrementally. The
   *   method {@link gobblin.configuration.SourceState#getPreviousWorkUnitStates} can be used to get the list of {@link WorkUnit}s
   *   from the previous run.
   * </p>
   *
   * @param state see {@link gobblin.configuration.SourceState}
   * @return a list of {@link WorkUnit}s
   */
  List<T> getWorkunits(SourceState state);


  /**
   * Shutdown this {@link DataSource} instance.
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
  public abstract void shutdown(SourceState state);

}
