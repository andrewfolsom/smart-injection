import { Component } from '@angular/core';
import { WellService } from './well-service.service'

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'smartinjection-app';
  wells;

  constructor(private wellService: WellService) {
  }

  ngOnInit() {
    console.log('In init');
    this.wellService.getWells().subscribe(value => {
      this.wells = value;
    })
  }

}
