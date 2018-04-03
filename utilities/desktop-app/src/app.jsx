import React from 'react';
import { Grid, Image } from 'semantic-ui-react'

export default class App extends React.Component {
  render() {
    return (
      <Grid divided='vertically'>
      <Grid.Row columns={2}>
        <Grid.Column>
          HAHAHAHA
        </Grid.Column>
        <Grid.Column>
          HE
        </Grid.Column>
      </Grid.Row>
  
      <Grid.Row columns={3}>
        <Grid.Column>
          HU
        </Grid.Column>
        <Grid.Column>
          KO
        </Grid.Column>
        <Grid.Column>
          BA
        </Grid.Column>
      </Grid.Row>
    </Grid>
    );
  }
}
