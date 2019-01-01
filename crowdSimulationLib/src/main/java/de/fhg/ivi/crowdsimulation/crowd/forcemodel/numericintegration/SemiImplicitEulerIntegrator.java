package de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.boundaries.BoundarySegment;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.ForceModel;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;

/**
 * Semi-Implicit Euler or Backward Euler is one of many possible algorithms of numerical integration
 * which can dissolve ordinary differential equations, like the Social Force Model
 * {@link ForceModel}. The difference between this class and the {@link SimpleEulerIntegrator} is,
 * that the position of the {@link Pedestrian} is updated in the same time step in this class. For
 * further explanation see the links below.
 * <p>
 * Euler explanation: <br>
 * http://codeflow.org/entries/2010/aug/28/integration-by-example-euler-vs-verlet-vs-runge-kutta/<br>
 * https://en.wikipedia.org/wiki/Semi-implicit_Euler_method<br>
 * https://scicomp.stackexchange.com/questions/20172/why-are-runge-kutta-and-eulers-method-so-different<br>
 *
 * @author hahmann/meinert
 */
public class SemiImplicitEulerIntegrator extends NumericIntegrator
{

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger = LoggerFactory.getLogger(SemiImplicitEulerIntegrator.class);

    /**
     * Overrides the abstract method in {@link NumericIntegrator} and sets this
     * {@link SemiImplicitEulerIntegrator} as algorithm of numeric integration, which dissolves the
     * {@link ForceModel}.
     * <p>
     * Basically this class calculates the movement of a specific {@link Pedestrian}. This means
     * his/her new position and velocity, in dependence to his/her old velocity and the terms of the
     * Social Force Model (see Helbing et al. 2005), is computed.
     *
     * @param time the time in simulated time, given in milliseconds
     * @param simulationInterval the time between two consecutive moves, given in seconds
     * @param pedestrian the one {@link Pedestrian}, whose movement is calculated
     * @param quadtree the {@link Quadtree} object allowing to access {@link Boundary} objects
     *
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.NumericIntegrator#move(long,
     *      double, Pedestrian, Quadtree)
     */
    @Override
    public void move(long time, double simulationInterval, Pedestrian pedestrian, Quadtree quadtree)
    {
        Vector2D resultingForce = pedestrian.getForces(time);

        // set velocity to zero, if the intrinsic force is null
        Vector2D currentVelocity = pedestrian.getCurrentVelocityVector();

        // updatedVelocity = v(n+1)
        Vector2D updatedVelocity = currentVelocity.add(resultingForce.multiply(simulationInterval));

        // check if updatedVelocity is bigger than maximal desired velocity
        updatedVelocity = NumericIntegrationTools.getValidatedVelocity(pedestrian, updatedVelocity);

        // old position
        Vector2D currentPosition = pedestrian.getCurrentPositionVector();

        // updatedPosition = x(n+1)
        Vector2D updatedPosition = pedestrian.getCurrentPositionVector()
            .add(updatedVelocity.multiply(simulationInterval));

        // validated updated position - guaranteed not to go through a boundary
        List<BoundarySegment> boundaries = null;
        if (quadtree != null)
            boundaries = quadtree.getBoundarySegments(
                new Envelope(currentPosition.toCoordinate(), updatedPosition.toCoordinate()));
        updatedPosition = NumericIntegrationTools.validateMove(pedestrian, boundaries,
            currentPosition, updatedPosition);

        // update position
        pedestrian.setCurrentPositionVector(updatedPosition);

        // update the mental model (e.g. check if the current WayPoint has been passed)
        pedestrian.getActiveWayFindingModel().updateModel(time, currentPosition, updatedPosition);

        // check for course deviations
        pedestrian.getActiveWayFindingModel().checkCourse(pedestrian, time);

        // update velocity
        pedestrian.setCurrentVelocityVector(updatedVelocity);

        logger.trace("move(), updatedVelocity: " + updatedVelocity);
        logger.trace("move(), currentPosition: " + currentPosition + ", updatedPosition: "
            + updatedPosition);

        if (currentPosition.equals(updatedPosition))
        {
            logger.trace("did not move. simulation interval=" + simulationInterval
                + ", resulting force=" + resultingForce + ", updatedVelocity=" + updatedVelocity);
        }
    }
}
